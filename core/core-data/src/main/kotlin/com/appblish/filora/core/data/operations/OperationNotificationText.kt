package com.appblish.filora.core.data.operations

import android.content.Context
import com.appblish.filora.core.data.R
import com.appblish.filora.core.domain.model.FileOperationKind
import com.appblish.filora.core.domain.model.OperationProgress

internal object OperationNotificationText {
    fun title(
        context: Context,
        kind: FileOperationKind
    ): String =
        when (kind) {
            FileOperationKind.Copy -> context.getString(R.string.ops_title_copy)
            FileOperationKind.Move -> context.getString(R.string.ops_title_move)
            FileOperationKind.Delete -> context.getString(R.string.ops_title_delete)
        }

    /** One-line status under the title, e.g. `3 of 12 · report.pdf`. */
    fun status(
        context: Context,
        progress: OperationProgress
    ): String =
        when (progress) {
            is OperationProgress.Pending -> context.getString(R.string.ops_status_pending)
            is OperationProgress.Running -> {
                val index = progress.itemIndex + 1
                if (progress.currentName.isNotBlank()) {
                    context.getString(
                        R.string.ops_status_progress_named,
                        index,
                        progress.itemCount,
                        progress.currentName
                    )
                } else {
                    context.getString(R.string.ops_status_progress, index, progress.itemCount)
                }
            }
            is OperationProgress.Succeeded ->
                if (progress.processedCount == 1) {
                    context.getString(R.string.ops_status_succeeded_one)
                } else {
                    context.getString(R.string.ops_status_succeeded_many, progress.processedCount)
                }
            is OperationProgress.Failed -> context.getString(R.string.ops_status_failed)
            is OperationProgress.Cancelled -> context.getString(R.string.ops_status_cancelled)
        }

    /** Progress as an integer percent in `0..100`, for the notification bar. */
    fun percent(progress: OperationProgress.Running): Int = (progress.fraction * 100f).toInt().coerceIn(0, 100)
}
