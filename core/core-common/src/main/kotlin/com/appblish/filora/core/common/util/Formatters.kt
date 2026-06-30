package com.appblish.filora.core.common.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow

/** Human-readable formatters shared across modules. Pure JVM, fully unit-testable. */
object Formatters {
    private val SIZE_UNITS = arrayOf("B", "KB", "MB", "GB", "TB", "PB")

    /**
     * Formats a byte count using binary (1024) units, e.g. `1536` -> `"1.5 KB"`.
     * Negative inputs are treated as `0`.
     */
    fun formatSize(
        bytes: Long,
        locale: Locale = Locale.getDefault()
    ): String {
        if (bytes <= 0L) return "0 B"
        val digitGroups =
            (ln(bytes.toDouble()) / ln(1024.0))
                .toInt()
                .coerceIn(0, SIZE_UNITS.lastIndex)
        val value = bytes / 1024.0.pow(digitGroups)
        return if (digitGroups == 0) {
            "$bytes B"
        } else {
            String.format(locale, "%.1f %s", value, SIZE_UNITS[digitGroups])
        }
    }

    private val DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")

    /** Formats an epoch-millis timestamp in the given [zone]. */
    fun formatDate(
        epochMillis: Long,
        zone: ZoneId = ZoneId.systemDefault()
    ): String = DATE_FORMAT.withZone(zone).format(Instant.ofEpochMilli(epochMillis))

    /**
     * Renders a filesystem path as breadcrumb-friendly segments, collapsing the
     * common Android emulated-storage prefix to `Internal storage`.
     */
    fun pathSegments(path: String): List<String> =
        path
            .removePrefix("/storage/emulated/0")
            .removePrefix("/sdcard")
            .split('/')
            .filter { it.isNotBlank() }
            .let { segments ->
                if (path.startsWith("/storage/emulated/0") || path.startsWith("/sdcard")) {
                    listOf("Internal storage") + segments
                } else {
                    segments
                }
            }

    /** Compact relative age like `"3 min ago"` / `"2 days ago"` from [nowMillis]. */
    fun relativeAge(
        epochMillis: Long,
        nowMillis: Long
    ): String {
        val deltaSec = abs(nowMillis - epochMillis) / 1000
        return when {
            deltaSec < 60 -> "just now"
            deltaSec < 3600 -> "${deltaSec / 60} min ago"
            deltaSec < 86_400 -> "${deltaSec / 3600} hr ago"
            else -> "${deltaSec / 86_400} days ago"
        }
    }
}
