package com.appblish.filora.core.data.archive

import com.appblish.filora.core.common.result.OperationError
import com.appblish.filora.core.common.result.Result
import com.appblish.filora.core.domain.model.ConflictStrategy
import com.appblish.filora.core.domain.usecase.ConflictResolution
import com.appblish.filora.core.domain.usecase.ConflictResolver
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import kotlin.coroutines.cancellation.CancellationException

/**
 * Outcome of a completed extraction. [createdDirectories] counts only the explicit
 * directory entries materialised by this run; parent directories created implicitly
 * for a file entry are not counted here.
 */
data class ExtractionSummary(
    val extractedFiles: Int,
    val skippedFiles: Int,
    val createdDirectories: Int,
)

/**
 * Pure, framework-free ZIP extraction engine (FR-7.2). Reconstructs the archive's
 * nested directory tree under a destination directory and applies a
 * [ConflictStrategy] per file when an entry collides with something already on disk
 * (reusing [ConflictResolver] so the skip / replace / keep-both semantics match
 * copy/move exactly).
 *
 * Path-traversal ("zip-slip") is enforced here, per [ArchiveRepository][com.appblish.filora.core.domain.repository.ArchiveRepository]'s
 * contract: every entry is resolved against the canonical destination root and any
 * entry that would escape it (`../`, absolute, symlink-relative) fails the whole
 * extraction with [OperationError.Io] rather than writing outside the target.
 *
 * The engine is cooperative-cancellation aware via [isActive]; a worker passes
 * `!isStopped` so a user/WorkManager stop aborts mid-archive with a
 * [CancellationException], which is propagated rather than swallowed as success.
 */
class ZipExtractor {
    /** Extracts the archive at [archive] into [destinationDir]. */
    fun extract(
        archive: File,
        destinationDir: File,
        strategy: ConflictStrategy = ConflictStrategy.KeepBoth,
        isActive: () -> Boolean = { true },
    ): Result<ExtractionSummary> =
        try {
            archive.inputStream().use { stream ->
                extract(stream, destinationDir, strategy, isActive)
            }
        } catch (io: IOException) {
            Result.Error(OperationError.Io(io))
        }

    /**
     * Extracts a ZIP stream into [destinationDir]. The [source] stream is consumed
     * and closed by this call. [destinationDir] is created if it does not exist.
     */
    fun extract(
        source: InputStream,
        destinationDir: File,
        strategy: ConflictStrategy = ConflictStrategy.KeepBoth,
        isActive: () -> Boolean = { true },
    ): Result<ExtractionSummary> {
        val base = destinationDir.canonicalFile
        if (!base.isDirectory && !base.mkdirs()) {
            return Result.Error(OperationError.Io(IOException("Cannot create destination: ${base.path}")))
        }

        return try {
            val summary = ZipInputStream(source.buffered()).use { zip ->
                consume(zip, base, strategy, isActive)
            }
            Result.Success(summary)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (io: IOException) {
            Result.Error(OperationError.Io(io))
        }
    }

    private fun consume(
        zip: ZipInputStream,
        base: File,
        strategy: ConflictStrategy,
        isActive: () -> Boolean,
    ): ExtractionSummary {
        val counters = Counters()
        var entry = zip.nextEntry
        while (entry != null) {
            if (!isActive()) throw CancellationException("Extraction cancelled")
            processEntry(zip, base, entry, strategy, counters)
            zip.closeEntry()
            entry = zip.nextEntry
        }
        return ExtractionSummary(counters.extracted, counters.skipped, counters.createdDirectories)
    }

    private fun processEntry(
        zip: ZipInputStream,
        base: File,
        entry: ZipEntry,
        strategy: ConflictStrategy,
        counters: Counters,
    ) {
        // A traversal entry escaping the base fails the whole archive (mapped to Io by the caller).
        val target = resolveWithinBase(base, entry.name)
            ?: throw IOException("Zip entry escapes target: ${entry.name}")
        if (isDirectoryEntry(entry)) {
            if (materialiseDirectory(target)) counters.createdDirectories++
        } else {
            when (writeFileEntry(zip, target, strategy)) {
                WriteResult.Written -> counters.extracted++
                WriteResult.Skipped -> counters.skipped++
            }
        }
    }

    private fun materialiseDirectory(target: File): Boolean {
        if (target.isDirectory) return false
        target.mkdirs()
        return target.isDirectory
    }

    private fun writeFileEntry(
        zip: ZipInputStream,
        target: File,
        strategy: ConflictStrategy,
    ): WriteResult {
        val parent = target.parentFile ?: return WriteResult.Skipped
        if (!parent.isDirectory) parent.mkdirs()

        val resolution = ConflictResolver.resolve(
            sourceName = target.name,
            isDirectory = false,
            existingNames = childNames(parent),
            strategy = strategy,
        )
        return when (resolution) {
            ConflictResolution.Skip -> WriteResult.Skipped
            is ConflictResolution.Proceed -> {
                File(parent, resolution.name).outputStream().use { out -> zip.copyTo(out) }
                WriteResult.Written
            }
        }
    }

    /**
     * Resolves [entryName] under [base] and returns the target only if it stays
     * inside [base]; `null` signals a path-traversal attempt. [base] is assumed
     * already canonical.
     */
    private fun resolveWithinBase(
        base: File,
        entryName: String,
    ): File? {
        val normalised = entryName.replace('\\', '/').trimStart('/')
        if (normalised.isEmpty()) return base
        val candidate = File(base, normalised).canonicalFile
        val basePath = base.path
        val inside = candidate.path == basePath || candidate.path.startsWith(basePath + File.separator)
        return if (inside) candidate else null
    }

    private fun childNames(dir: File): Set<String> = dir.list()?.toSet() ?: emptySet()

    private fun isDirectoryEntry(entry: ZipEntry): Boolean = entry.isDirectory || entry.name.endsWith("/")

    private enum class WriteResult { Written, Skipped }

    /** Mutable running tally threaded through the entry loop. */
    private class Counters {
        var extracted = 0
        var skipped = 0
        var createdDirectories = 0
    }
}
