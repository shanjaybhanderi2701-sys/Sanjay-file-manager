package com.appblish.filora.core.data.archive

import com.appblish.filora.core.common.result.OperationError
import com.appblish.filora.core.common.result.Result
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.coroutines.cancellation.CancellationException

/** Outcome of a completed compression: how many file entries were written. */
data class CompressionSummary(
    val entryCount: Int,
)

/**
 * Pure, framework-free ZIP compression engine (FR-7.1) — the symmetric counterpart
 * of [ZipExtractor]. Flattens one or more source files/directories into a single
 * ZIP at a destination path, preserving each source's own folder name as the entry
 * prefix (zipping `foo/` yields `foo/a.txt`, `foo/sub/b.txt`).
 *
 * The destination tree is pre-walked once so the total file count is known before
 * any bytes are written; the worker uses that to publish a determinate
 * [ArchiveProgress.Running][com.appblish.filora.core.domain.model.ArchiveProgress.Running].
 *
 * Cancellation safety (the cancel-cleans-partial-output AC): the engine is
 * cooperative-cancellation aware via [isActive] (a worker passes `!isStopped`),
 * polled before each entry. On cancellation — or on any [IOException] — the
 * half-written destination file is deleted so a stop never leaves a corrupt
 * archive behind. A [CancellationException] is propagated rather than swallowed as
 * success.
 */
class ZipCompressor {
    /**
     * Compresses [sources] into a new ZIP at [destination]. [destination]'s parent
     * directories are created if needed; an existing file at [destination] is
     * overwritten. [onProgress] is invoked once per file entry with the
     * zero-based index, the total file count and the entry's display name.
     */
    fun compress(
        sources: List<File>,
        destination: File,
        onProgress: (index: Int, total: Int, name: String) -> Unit = { _, _, _ -> },
        isActive: () -> Boolean = { true },
    ): Result<CompressionSummary> {
        if (sources.isEmpty()) {
            return Result.Error(OperationError.InvalidName())
        }
        val missing = sources.firstOrNull { !it.exists() }
        if (missing != null) {
            return Result.Error(OperationError.NotFound(path = missing.path))
        }

        val parent = destination.canonicalFile.parentFile
        if (parent != null && !parent.isDirectory && !parent.mkdirs()) {
            return Result.Error(OperationError.Io(IOException("Cannot create destination dir: ${parent.path}")))
        }

        val planned = sources.flatMap(::planEntries)
        val fileEntries = planned.filter { !it.isDirectory }

        return try {
            writeArchive(destination, planned, fileEntries.size, onProgress, isActive)
            Result.Success(CompressionSummary(entryCount = fileEntries.size))
        } catch (cancellation: CancellationException) {
            destination.delete()
            throw cancellation
        } catch (io: IOException) {
            destination.delete()
            Result.Error(OperationError.Io(io))
        }
    }

    private fun writeArchive(
        destination: File,
        planned: List<PlannedEntry>,
        totalFiles: Int,
        onProgress: (Int, Int, String) -> Unit,
        isActive: () -> Boolean,
    ) {
        var fileIndex = 0
        ZipOutputStream(destination.outputStream().buffered()).use { zip ->
            for (entry in planned) {
                if (!isActive()) throw CancellationException("Compression cancelled")
                if (entry.isDirectory) {
                    writeDirectoryEntry(zip, entry)
                } else {
                    onProgress(fileIndex, totalFiles, entry.file.name)
                    writeFileEntry(zip, entry)
                    fileIndex++
                }
            }
        }
    }

    private fun writeDirectoryEntry(
        zip: ZipOutputStream,
        entry: PlannedEntry,
    ) {
        zip.putNextEntry(ZipEntry(entry.name))
        zip.closeEntry()
    }

    private fun writeFileEntry(
        zip: ZipOutputStream,
        entry: PlannedEntry,
    ) {
        zip.putNextEntry(ZipEntry(entry.name))
        entry.file.inputStream().use { input -> input.copyTo(zip) }
        zip.closeEntry()
    }

    /**
     * Flattens [source] into the ZIP entries it contributes. A regular file becomes
     * a single entry named after itself; a directory becomes one entry per
     * descendant, each prefixed with the directory's own name, plus explicit
     * directory entries (so empty directories survive a round-trip).
     */
    private fun planEntries(source: File): List<PlannedEntry> {
        if (source.isFile) {
            return listOf(PlannedEntry(source, source.name, isDirectory = false))
        }
        val prefixBase = source.parentFile ?: source
        val entries = mutableListOf<PlannedEntry>()
        source.walkTopDown().forEach { node ->
            val relative = node.toRelativeString(prefixBase).replace(File.separatorChar, '/')
            when {
                node == source && node.isDirectory && isEmptyDir(node) ->
                    entries += PlannedEntry(node, "$relative/", isDirectory = true)
                node.isDirectory && isEmptyDir(node) ->
                    entries += PlannedEntry(node, "$relative/", isDirectory = true)
                node.isFile ->
                    entries += PlannedEntry(node, relative, isDirectory = false)
                else -> Unit // non-empty directory: its children carry the path
            }
        }
        return entries
    }

    private fun isEmptyDir(dir: File): Boolean = dir.listFiles()?.isEmpty() ?: true

    private data class PlannedEntry(
        val file: File,
        val name: String,
        val isDirectory: Boolean,
    )
}
