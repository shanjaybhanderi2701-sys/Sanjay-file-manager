package com.appblish.filora.core.data.operations

import android.app.Notification
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo

/**
 * Foreground notification for the ZIP compress worker (FR-7.1). Reuses the shared
 * `ops` channel created by [OperationNotifier] so compression shares the same
 * low-importance progress lane as copy/move/delete and extract, but carries its
 * own notification id.
 *
 * Unlike the extract notifier (indeterminate — a streamed archive has no up-front
 * entry count), compression pre-walks its sources, so the bar is determinate once
 * the first [foregroundInfo] is rebuilt with a known total.
 */
internal class ArchiveCompressNotifier(
    private val context: Context,
) {
    private val channel = OperationNotifier(context)

    /** Indeterminate info used for the initial foreground promotion before any progress. */
    fun foregroundInfo(): ForegroundInfo = wrap(buildNotification(processed = 0, total = 0))

    /** Determinate info reflecting [processed] of [total] entries written. */
    fun foregroundInfo(
        processed: Int,
        total: Int,
    ): ForegroundInfo = wrap(buildNotification(processed, total))

    private fun wrap(notification: Notification): ForegroundInfo =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }

    private fun buildNotification(
        processed: Int,
        total: Int,
    ): Notification {
        channel.ensureChannel()
        val builder = NotificationCompat
            .Builder(context, OperationNotifier.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle("Compressing archive")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
        if (total > 0) {
            builder.setProgress(total, processed, false)
        } else {
            builder.setProgress(0, 0, true)
        }
        return builder.build()
    }

    companion object {
        const val NOTIFICATION_ID = 0xC02E
    }
}
