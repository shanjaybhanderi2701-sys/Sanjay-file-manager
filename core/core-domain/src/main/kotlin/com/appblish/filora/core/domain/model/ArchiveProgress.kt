package com.appblish.filora.core.domain.model

import com.appblish.filora.core.common.result.OperationError

/**
 * UI/worker-facing progress for a long-running ZIP compression that runs in the
 * background via WorkManager (FR-7.1). The compress worker publishes this through
 * its progress channel and re-emits it as the work transitions; the browser
 * observes it to drive the foreground notification and the in-app progress row.
 *
 * Because WorkManager persists enqueued work, a compression — and the
 * [ArchiveProgress] stream observing it — survives process death and resumes
 * reporting once the app is back in the foreground (NFR-2.3).
 *
 * Compression is the symmetric counterpart of [OperationProgress]; it is kept as a
 * distinct type because the unit of work is a ZIP *entry* (a flattened file in the
 * source tree) rather than a top-level copy/move/delete item, and because the total
 * entry count is known up front, so [Running] can report a determinate [fraction].
 */
sealed interface ArchiveProgress {
    /** Enqueued but not yet started (WorkManager is scheduling or blocked). */
    data object Pending : ArchiveProgress

    /**
     * The archive is being written. [processedEntries] is the number of source
     * files already added to the archive out of [totalEntries]; [currentName] is
     * the display name of the entry currently being written. [totalEntries] is the
     * pre-walked count of regular files across all sources, so a determinate bar
     * can be shown.
     */
    data class Running(
        val processedEntries: Int,
        val totalEntries: Int,
        val currentName: String,
    ) : ArchiveProgress {
        /** Completion in `0f..1f`; `0f` when the entry count is not yet known. */
        val fraction: Float
            get() = if (totalEntries > 0) {
                (processedEntries.toFloat() / totalEntries).coerceIn(0f, 1f)
            } else {
                0f
            }
    }

    /** The archive was written successfully at the chosen destination. */
    data class Succeeded(
        val archivePath: String,
        val entryCount: Int,
    ) : ArchiveProgress

    /** The compression failed; [error] is the failure that stopped it. */
    data class Failed(
        val error: OperationError,
    ) : ArchiveProgress

    /** The user cancelled before the archive finished; partial output is removed. */
    data object Cancelled : ArchiveProgress
}
