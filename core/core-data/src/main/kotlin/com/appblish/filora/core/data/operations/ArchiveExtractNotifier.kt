package com.appblish.filora.core.data.operations

import android.app.Notification
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
import com.appblish.filora.core.data.R

/**
 * Foreground notification for the ZIP extract worker (FR-7.2). Reuses the shared
 * `ops` channel created by [OperationNotifier] so extraction shares the same
 * low-importance progress lane as copy/move/delete, but carries its own
 * notification id and an indeterminate progress bar (entry counts are not known
 * up front for a streamed archive).
 */
internal class ArchiveExtractNotifier(
    private val context: Context,
) {
    private val channel = OperationNotifier(context)

    fun foregroundInfo(): ForegroundInfo {
        channel.ensureChannel()
        val notification = buildNotification()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(): Notification =
        NotificationCompat
            .Builder(context, OperationNotifier.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(context.getString(R.string.ops_title_extract))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setProgress(0, 0, true)
            .build()

    companion object {
        const val NOTIFICATION_ID = 0xF12E
    }
}
