package com.appblish.filora.core.data.operations

import androidx.work.Data
import androidx.work.workDataOf
import com.appblish.filora.core.common.result.OperationError
import com.appblish.filora.core.data.archive.CompressionSummary
import com.appblish.filora.core.domain.model.ArchiveProgress

/**
 * Translation between the typed compress-worker arguments/results and the
 * loosely-typed WorkManager [Data] bags. Framework-free so the encoding stays
 * unit-testable, mirroring [ArchiveExtractWorkData] and [OperationWorkData].
 *
 * Reuses [wireTag]/[operationErrorFromTag] from the operations package so a failed
 * compression surfaces the same stable error tags the rest of the UI already maps.
 *
 * Source paths travel inline as a string array. ZIP compression takes an explicit
 * user-chosen set of files/folders (FR-7.1) rather than the unbounded selections
 * the copy/move workers face, so there is no overflow-to-[WorkRequestStore] path
 * here; an oversized argument bag fails fast at enqueue time like any other worker.
 */
internal object ArchiveCompressWorkData {
    // ---- input keys ----
    private const val KEY_SOURCES = "cz_sources"
    private const val KEY_DEST = "cz_dest"

    // ---- progress keys ----
    private const val KEY_PROCESSED = "cz_processed"
    private const val KEY_TOTAL = "cz_total"
    private const val KEY_CURRENT = "cz_current"

    // ---- terminal output keys ----
    private const val KEY_ENTRY_COUNT = "cz_entries"
    private const val KEY_ERROR = "cz_error"

    fun encodeInput(args: ArchiveCompressArgs): Data =
        workDataOf(
            KEY_SOURCES to args.sources.toTypedArray(),
            KEY_DEST to args.destinationArchivePath,
        )

    /** Rebuilds the worker arguments; `null` when sources or destination are absent/empty. */
    fun decodeInput(data: Data): ArchiveCompressArgs? {
        val sources = data.getStringArray(KEY_SOURCES)?.toList().orEmpty()
        val destination = data.getString(KEY_DEST) ?: return null
        if (sources.isEmpty() || destination.isBlank()) return null
        return ArchiveCompressArgs(sources, destination)
    }

    fun encodeProgress(progress: ArchiveProgress.Running): Data =
        workDataOf(
            KEY_PROCESSED to progress.processedEntries,
            KEY_TOTAL to progress.totalEntries,
            KEY_CURRENT to progress.currentName,
        )

    /** Reads back a [ArchiveProgress.Running] from a running worker's progress data. */
    fun decodeProgress(data: Data): ArchiveProgress.Running =
        ArchiveProgress.Running(
            processedEntries = data.getInt(KEY_PROCESSED, 0),
            totalEntries = data.getInt(KEY_TOTAL, 0),
            currentName = data.getString(KEY_CURRENT).orEmpty(),
        )

    fun encodeSuccess(
        destinationArchivePath: String,
        summary: CompressionSummary,
    ): Data =
        workDataOf(
            KEY_DEST to destinationArchivePath,
            KEY_ENTRY_COUNT to summary.entryCount,
        )

    /** Reads back the [ArchiveProgress.Succeeded] from a succeeded worker's output. */
    fun decodeSuccess(data: Data): ArchiveProgress.Succeeded =
        ArchiveProgress.Succeeded(
            archivePath = data.getString(KEY_DEST).orEmpty(),
            entryCount = data.getInt(KEY_ENTRY_COUNT, 0),
        )

    fun encodeFailure(error: OperationError): Data = workDataOf(KEY_ERROR to error.wireTag())

    /** Reads back the failure error from a failed worker's output. */
    fun decodeError(data: Data): OperationError = operationErrorFromTag(data.getString(KEY_ERROR))
}

/** Decoded arguments an [ArchiveCompressWorker] runs against. */
internal data class ArchiveCompressArgs(
    val sources: List<String>,
    val destinationArchivePath: String,
)
