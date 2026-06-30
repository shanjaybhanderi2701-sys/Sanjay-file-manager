package com.appblish.filora.core.data.operations

import androidx.work.Data
import androidx.work.workDataOf
import com.appblish.filora.core.common.result.OperationError
import com.appblish.filora.core.data.archive.ExtractionSummary
import com.appblish.filora.core.domain.model.ConflictStrategy

/**
 * Translation between the typed extract-worker arguments/results and the
 * loosely-typed WorkManager [Data] bags. Framework-free so the encoding stays
 * unit-testable, mirroring [OperationWorkData] for the copy/move/delete workers.
 *
 * Reuses [wireTag]/[operationErrorFromTag] from the operations package so a failed
 * extraction surfaces the same stable error tags the rest of the UI already maps.
 */
internal object ArchiveExtractWorkData {
    private const val KEY_ARCHIVE = "ax_archive"
    private const val KEY_DEST = "ax_dest"
    private const val KEY_STRATEGY = "ax_strategy"

    private const val KEY_EXTRACTED = "ax_extracted"
    private const val KEY_SKIPPED = "ax_skipped"
    private const val KEY_DIRS = "ax_dirs"
    private const val KEY_ERROR = "ax_error"

    fun encodeInput(args: ArchiveExtractArgs): Data =
        workDataOf(
            KEY_ARCHIVE to args.archivePath,
            KEY_DEST to args.destinationDir,
            KEY_STRATEGY to args.strategy.name,
        )

    /** Rebuilds the worker arguments; `null` when the required paths are absent. */
    fun decodeInput(data: Data): ArchiveExtractArgs? {
        val archive = data.getString(KEY_ARCHIVE) ?: return null
        val destination = data.getString(KEY_DEST) ?: return null
        val strategy = data
            .getString(KEY_STRATEGY)
            ?.let { runCatching { ConflictStrategy.valueOf(it) }.getOrNull() }
            ?: ConflictStrategy.KeepBoth
        return ArchiveExtractArgs(archive, destination, strategy)
    }

    fun encodeSuccess(summary: ExtractionSummary): Data =
        workDataOf(
            KEY_EXTRACTED to summary.extractedFiles,
            KEY_SKIPPED to summary.skippedFiles,
            KEY_DIRS to summary.createdDirectories,
        )

    fun encodeFailure(error: OperationError): Data = workDataOf(KEY_ERROR to error.wireTag())

    /** Reads back the [ExtractionSummary] from a succeeded worker's output. */
    fun decodeSummary(data: Data): ExtractionSummary =
        ExtractionSummary(
            extractedFiles = data.getInt(KEY_EXTRACTED, 0),
            skippedFiles = data.getInt(KEY_SKIPPED, 0),
            createdDirectories = data.getInt(KEY_DIRS, 0),
        )

    /** Reads back the failure error from a failed worker's output. */
    fun decodeError(data: Data): OperationError = operationErrorFromTag(data.getString(KEY_ERROR))
}

/** Decoded arguments an [ArchiveExtractWorker] runs against. */
internal data class ArchiveExtractArgs(
    val archivePath: String,
    val destinationDir: String,
    val strategy: ConflictStrategy,
)
