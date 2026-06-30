package com.appblish.filora.core.domain.model

import com.appblish.filora.core.common.result.OperationError

/** The kind of long-running file operation a background worker carries out (FR-3.5). */
enum class FileOperationKind {
    Copy,
    Move,
    Delete,
}

/**
 * UI/worker-facing progress for a long, large operation that runs in the
 * background via WorkManager (FR-3.5). Workers publish this through their
 * progress channel and re-emit it as the work transitions; the browser observes
 * it to drive the foreground notification and the in-app progress row.
 *
 * Because WorkManager persists enqueued work, an operation — and the
 * [OperationProgress] stream observing it — survives process death and resumes
 * reporting once the app is back in the foreground (NFR-2.3). This type is
 * deliberately free of `completedBytes`/`totalBytes` precision guarantees: a
 * source whose total size is unknown reports [Running.totalBytes] as `0`, and
 * consumers fall back to item-count granularity.
 */
sealed interface OperationProgress {
    /** The operation that this progress describes. */
    val kind: FileOperationKind

    /** Enqueued but not yet started (WorkManager is scheduling or blocked). */
    data class Pending(
        override val kind: FileOperationKind,
    ) : OperationProgress

    /**
     * The operation is running. [itemIndex] is the zero-based index of the item
     * currently being processed out of [itemCount]; [currentName] is its display
     * name. Byte counters are best-effort and may be `0` when a source size is
     * unknown.
     */
    data class Running(
        override val kind: FileOperationKind,
        val itemIndex: Int,
        val itemCount: Int,
        val currentName: String,
        val completedBytes: Long = 0L,
        val totalBytes: Long = 0L,
    ) : OperationProgress {
        /**
         * Completion in `0f..1f`. Prefers byte granularity when [totalBytes] is
         * known, otherwise falls back to item granularity. Returns `0f` when
         * neither is available.
         */
        val fraction: Float
            get() = when {
                totalBytes > 0L -> (completedBytes.toFloat() / totalBytes).coerceIn(0f, 1f)
                itemCount > 0 -> (itemIndex.toFloat() / itemCount).coerceIn(0f, 1f)
                else -> 0f
            }
    }

    /** Every requested item was processed successfully. */
    data class Succeeded(
        override val kind: FileOperationKind,
        val processedCount: Int,
    ) : OperationProgress

    /** The operation failed; [error] is the first failure that stopped it. */
    data class Failed(
        override val kind: FileOperationKind,
        val error: OperationError,
    ) : OperationProgress

    /** The user cancelled the operation before it finished. */
    data class Cancelled(
        override val kind: FileOperationKind,
    ) : OperationProgress
}
