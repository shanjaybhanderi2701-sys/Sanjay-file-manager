package com.appblish.filora.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Resolves the storage-read permissions Filora requests at runtime and reports
 * whether they are currently granted.
 *
 * Strategy (see docs/phase-1/04-technical-architecture.md §5): the Play-default
 * "standard" build is least-privilege and never requests all-files access. On
 * API 33+ we ask for the granular `READ_MEDIA_*` permissions; on API ≤32 the
 * single `READ_EXTERNAL_STORAGE`. Everything outside shared media is reached
 * through SAF, which needs no runtime permission — so a denial still leaves a
 * usable browsing path (wired in T1.3).
 */
object StoragePermissions {
    /**
     * The read permissions to request at runtime for [sdkInt]. Pure and
     * level-parameterised so the mapping is unit-testable on the JVM.
     */
    fun requiredReadPermissions(sdkInt: Int = Build.VERSION.SDK_INT): List<String> =
        if (sdkInt >= Build.VERSION_CODES.TIRAMISU) {
            listOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO,
            )
        } else {
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

    /**
     * True once the user has granted media read access. On API 34+ a partial
     * ("selected photos only") grant — surfaced via
     * `READ_MEDIA_VISUAL_USER_SELECTED` — also counts as access so we don't nag
     * users who deliberately chose limited sharing.
     */
    fun hasMediaAccess(
        context: Context,
        sdkInt: Int = Build.VERSION.SDK_INT,
    ): Boolean {
        if (sdkInt >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            isGranted(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
        ) {
            return true
        }
        return requiredReadPermissions(sdkInt).all { isGranted(context, it) }
    }

    private fun isGranted(
        context: Context,
        permission: String,
    ): Boolean =
        ContextCompat.checkSelfPermission(context, permission) ==
            PackageManager.PERMISSION_GRANTED
}
