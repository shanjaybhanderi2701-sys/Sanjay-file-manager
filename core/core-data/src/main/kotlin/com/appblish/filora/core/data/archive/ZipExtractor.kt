package com.appblish.filora.core.data.archive

import com.appblish.filora.core.common.result.OperationError
import com.appblish.filora.core.common.result.Result
import com.appblish.filora.core.domain.model.ConflictStrategy
import com.appblish.filora.core.domain.usecase.ConflictResolution
import com.appblish.filora.core.domain.usecase.ConflictResolver
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
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
 * Resource ceilings enforced during extraction to defend against zip-bomb
 * storage-exhaustion (security-impl-audit F4). All three are checked incrementally as
 * bytes are written, so a bomb aborts mid-stream rather than after filling the disk.
 *
 * @param maxTotalUncompressedBytes cap on the sum of all uncompressed output for one archive.
 * @param maxEntryUncompressedBytes cap on a single entry's uncompressed output.
 * @param maxCompressionRatio cap on uncompressed/compressed for entries whose sizes are
 *   declared up-front (STORED entries and any zip carrying a valid local header).
 */
data class ExtractionLimits(
    val maxTotalUncompressedBytes: Long,
    val maxEntryUncompressedBytes: Long,
    val maxCompressionRatio: Long,
) {
    companion object {
        /** Generous production defaults — real user archives sit far below these. */
        val Default = ExtractionLimits(
            maxTotalUncompressedBytes = 4L * 1024 * 1024 * 1024, // 4 GiB per archive
            maxEntryUncompressedBytes = 2L * 1024 * 1024 * 1024, // 2 GiB per file
            maxCompressionRatio = 200L,
        )
    }
}

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
 *
 * Zip-bomb storage-exhaustion is bounded by [limits] (security-impl-audit F4): the
 * running uncompressed total, any single entry, and (where declared) the compression
 * ratio are checked as bytes stream out, so an oversized archive fails with
 * [OperationError.Io] instead of filling the device. Symlink entries need no special
 * handling: `java.util.zip` never materialises a symlink — it writes the entry's bytes
 * as a regular file — so a symlinked escape is impossible, and traversal is already
 * blocked by [resolveWithinBase].
 */
class ZipExtractor(
    private val limits: ExtractionLimits = ExtractionLimits.Default,
) {
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
        val guard = SizeGuard()
        var entry = zip.nextEntry
        while (entry != null) {
            if (!isActive()) throw CancellationException("Extraction cancelled")
            processEntry(zip, base, entry, strategy, counters, guard)
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
        guard: SizeGuard,
    ) {
        // A traversal entry escaping the base fails the whole archive (mapped to Io by the caller).
        val target = resolveWithinBase(base, entry.name)
            ?: throw IOException("Zip entry escapes target: ${entry.name}")
        if (isDirectoryEntry(entry)) {
            if (materialiseDirectory(target)) counters.createdDirectories++
        } else {
            when (writeFileEntry(zip, entry, target, strategy, guard)) {
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
        entry: ZipEntry,
        target: File,
        strategy: ConflictStrategy,
        guard: SizeGuard,
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
                guard.beginEntry(entry)
                File(parent, resolution.name).outputStream().use { out -> copyGuarded(zip, out, guard) }
                WriteResult.Written
            }
        }
    }

    /**
     * Streams [zip]'s current entry into [out], accounting every chunk against [guard] so
     * an oversized entry/archive aborts mid-copy (before the disk fills) with an
     * [IOException] the caller maps to [OperationError.Io].
     */
    private fun copyGuarded(
        zip: ZipInputStream,
        out: OutputStream,
        guard: SizeGuard,
    ) {
        val buffer = ByteArray(COPY_BUFFER_BYTES)
        while (true) {
            val read = zip.read(buffer)
            if (read < 0) break
            out.write(buffer, 0, read)
            guard.account(read.toLong())
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

    /**
     * Enforces [limits] as bytes stream out (security-impl-audit F4). [beginEntry] does
     * the cheap up-front checks for entries that declare their sizes; [account] tracks the
     * per-entry and archive-wide running totals so an entry that only reveals its true
     * size while inflating still aborts before exhausting storage. `inner` so it reads the
     * enclosing extractor's [limits].
     */
    private inner class SizeGuard {
        private var total = 0L
        private var currentEntry = 0L

        fun beginEntry(entry: ZipEntry) {
            currentEntry = 0L
            val declared = entry.size
            val compressed = entry.compressedSize
            if (declared > 0 && declared > limits.maxEntryUncompressedBytes) {
                throw IOException("Zip entry '${entry.name}' declares $declared B over the per-entry cap")
            }
            if (declared > 0 && compressed > 0 && declared / compressed > limits.maxCompressionRatio) {
                throw IOException("Zip entry '${entry.name}' compression ratio over cap")
            }
        }

        fun account(bytes: Long) {
            currentEntry += bytes
            total += bytes
            if (currentEntry > limits.maxEntryUncompressedBytes) {
                throw IOException("Zip entry exceeds the per-entry uncompressed cap")
            }
            if (total > limits.maxTotalUncompressedBytes) {
                throw IOException("Archive exceeds the total uncompressed cap")
            }
        }
    }

    private companion object {
        const val COPY_BUFFER_BYTES = 8 * 1024
    }
}
