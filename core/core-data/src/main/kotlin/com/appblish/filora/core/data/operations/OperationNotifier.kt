package com.appblish.filora.core.data.operations

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import androidx.work.ForegroundInfo
import com.appblish.filora.core.data.R
import com.appblish.filora.core.domain.model.FileOperationKind
import com.appblish.filora.core.domain.model.OperationProgress

/**
 * Builds the `ops` foreground-notification channel and the per-tick
 * [ForegroundInfo] a worker promotes itself with (FR-3.5). Keeping it a tiny,
 * injectable seam lets the workers stay focused on the operation loop and lets
 * the channel be created exactly once, lazily, on first use.
 *
 * The actual copy decisions live in [OperationNotificationText] so they can be
 * tested without an Android `Context`.
 */
internal class OperationNotifier(
    private val context: Context,
) {
    private val manager: NotificationManager? = context.getSystemService()

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = manager ?: return
        if (mgr.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.ops_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.ops_channel_description)
            setShowBadge(false)
        }
        mgr.createNotificationChannel(channel)
    }

    fun foregroundInfo(progress: OperationProgress): ForegroundInfo {
        ensureChannel()
        val notification = buildNotification(progress)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(progress: OperationProgress): Notification {
        val kind: FileOperationKind = progress.kind
        val builder = NotificationCompat
            .Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(OperationNotificationText.title(context, kind))
            .setContentText(OperationNotificationText.status(context, progress))
            .setOngoing(progress is OperationProgress.Pending || progress is OperationProgress.Running)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        when (progress) {
            is OperationProgress.Pending -> builder.setProgress(0, 0, true)
            is OperationProgress.Running -> {
                if (progress.totalBytes > 0L || progress.itemCount > 0) {
                    builder.setProgress(100, OperationNotificationText.percent(progress), false)
                } else {
                    builder.setProgress(0, 0, true)
                }
            }
            else -> builder.setProgress(0, 0, false)
        }
        return builder.build()
    }

    companion object {
        const val CHANNEL_ID = "ops"
        const val NOTIFICATION_ID = 0xF11E
    }
}
