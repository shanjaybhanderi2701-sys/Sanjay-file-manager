package com.appblish.filora.core.data.operations

import com.appblish.filora.core.domain.model.FileOperationKind
import com.appblish.filora.core.domain.model.OperationProgress

/**
 * Pure (Android-free) formatting of the foreground-notification copy for a file
 * operation, split out from [OperationNotifier] so the wording is unit-testable
 * without a `Context`. The strings are intentionally plain English here; once a
 * string-resource catalogue exists these move behind resource ids.
 */
internal object OperationNotificationText {
    fun title(kind: FileOperationKind): String =
        when (kind) {
            FileOperationKind.Copy -> "Copying files"
            FileOperationKind.Move -> "Moving files"
            FileOperationKind.Delete -> "Deleting files"
        }

    /** One-line status under the title, e.g. `3 of 12 · report.pdf`. */
    fun status(progress: OperationProgress): String =
        when (progress) {
            is OperationProgress.Pending -> "Preparing…"
            is OperationProgress.Running -> buildString {
                append(progress.itemIndex + 1)
                append(" of ")
                append(progress.itemCount)
                if (progress.currentName.isNotBlank()) {
                    append(" · ")
                    append(progress.currentName)
                }
            }
            is OperationProgress.Succeeded ->
                "Completed ${progress.processedCount} item" +
                    if (progress.processedCount == 1) "" else "s"
            is OperationProgress.Failed -> "Couldn't complete the operation"
            is OperationProgress.Cancelled -> "Cancelled"
        }

    /** Progress as an integer percent in `0..100`, for the notification bar. */
    fun percent(progress: OperationProgress.Running): Int = (progress.fraction * 100f).toInt().coerceIn(0, 100)
}
