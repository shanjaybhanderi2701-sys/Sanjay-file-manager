package com.appblish.filora.permission

import android.content.Intent
import android.net.Uri
import android.provider.Settings

/**
 * Deep-link into this app's system settings page (T028, FR-1.1).
 *
 * Once the OS marks a runtime permission *permanently* denied ("Don't ask again"),
 * `RequestMultiplePermissions` returns instantly without showing a dialog — the
 * only way back to a grant is the system app-info screen. The denied branch of the
 * permission gate surfaces this as an escape hatch so a permanent denial is never a
 * dead end; SAF browsing still works regardless.
 */
object AppSettings {
    /**
     * The `package:`-scheme data part for an app-details settings intent. Pure and
     * string-only so the mapping is unit-testable on the JVM without an Android
     * runtime (see [AppSettingsTest]).
     */
    fun packageUri(packageName: String): String = "package:$packageName"

    /**
     * Intent that opens the system "App info" screen for [packageName], where the
     * user can toggle storage/media permissions. Built from [packageUri] so the
     * tested string is the real source of truth.
     */
    fun appDetailsIntent(packageName: String): Intent =
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse(packageUri(packageName)),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
