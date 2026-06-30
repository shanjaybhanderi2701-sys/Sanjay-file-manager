package com.appblish.filora.permission

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import com.appblish.filora.BuildConfig

/**
 * All-files access (`MANAGE_EXTERNAL_STORAGE`) opt-in flow (T022, FR-1.1).
 *
 * This is **never** offered on the Play-default `standard` flavor: that build
 * neither declares the permission (manifest) nor sets the gate flag
 * ([BuildConfig.FULL_ACCESS_SUPPORTED] is `false`), so all-files access can never
 * be requested there (architecture §Permissions, decision A3). The opt-in `fullaccess`
 * flavor declares the permission and, after the user reads an explicit justification
 * screen, sends them to the system "All files access" toggle. The permission cannot
 * be granted by a runtime dialog — only via that settings screen — so we route there
 * directly.
 */
object AllFilesAccess {
    /** True only on a build variant that may request all-files access. */
    val isSupported: Boolean get() = BuildConfig.FULL_ACCESS_SUPPORTED

    /**
     * Whether to offer the all-files opt-in: only on a full-access build running
     * where the API exists (R+, where the permission and its settings screen were
     * introduced). Pure and parameterised so it is unit-testable on the JVM
     * (see [AllFilesAccessTest]).
     */
    fun shouldOffer(
        fullAccessSupported: Boolean = isSupported,
        sdkInt: Int = Build.VERSION.SDK_INT,
    ): Boolean = fullAccessSupported && sdkInt >= Build.VERSION_CODES.R

    /**
     * Intent to the system "All files access" screen pre-scoped to this app. The
     * app-scoped action lands directly on Filora's toggle rather than the global
     * list, which Play policy prefers for justified use.
     */
    fun manageAccessIntent(packageName: String): Intent =
        Intent(
            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
            Uri.parse("package:$packageName"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** True once the user has granted all-files access in system settings. */
    fun isGranted(sdkInt: Int = Build.VERSION.SDK_INT): Boolean =
        sdkInt >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()
}
