package com.appblish.filora.core.data.media

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.appblish.filora.core.domain.repository.MediaAccess
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * [MediaAccess] backed by the runtime permission state.
 *
 * Mirrors the request-side predicate in the app's `StoragePermissions` (they must
 * stay in sync): on API 33+ the granular `READ_MEDIA_*` reads, on API ≤32 the
 * single `READ_EXTERNAL_STORAGE`, and on API 34+ a partial "selected photos only"
 * grant ([Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED]) also counts so we
 * don't treat a deliberate limited share as "no access".
 *
 * Read live on every call (not cached) so Home/Media re-querying on resume picks up
 * a grant the user toggled in system settings while the app was backgrounded.
 */
class AndroidMediaAccess
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : MediaAccess {
        override fun hasReadAccess(): Boolean = hasReadAccess(Build.VERSION.SDK_INT)

        /** SDK-parameterised so the mapping is unit-testable on the JVM. */
        internal fun hasReadAccess(sdkInt: Int): Boolean {
            if (sdkInt >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                isGranted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
            ) {
                return true
            }
            return requiredReadPermissions(sdkInt).all { isGranted(it) }
        }

        private fun requiredReadPermissions(sdkInt: Int): List<String> =
            if (sdkInt >= Build.VERSION_CODES.TIRAMISU) {
                listOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_AUDIO,
                )
            } else {
                listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }

        private fun isGranted(permission: String): Boolean =
            ContextCompat.checkSelfPermission(context, permission) ==
                PackageManager.PERMISSION_GRANTED
    }
