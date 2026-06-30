package com.appblish.filora.permission

import android.Manifest
import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Verifies the level-appropriate read-permission mapping (FR-1.1). Pure JVM test —
 * [StoragePermissions.requiredReadPermissions] takes the SDK level as a parameter
 * so no device/Robolectric runtime is needed.
 */
class StoragePermissionsTest {
    @Test
    fun `api 33+ requests granular media permissions`() {
        val expected =
            listOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO,
            )

        assertEquals(expected, StoragePermissions.requiredReadPermissions(Build.VERSION_CODES.TIRAMISU))
        assertEquals(expected, StoragePermissions.requiredReadPermissions(Build.VERSION_CODES.VANILLA_ICE_CREAM))
    }

    @Test
    fun `api 32 and below request legacy read external storage`() {
        val expected = listOf(Manifest.permission.READ_EXTERNAL_STORAGE)

        assertEquals(expected, StoragePermissions.requiredReadPermissions(Build.VERSION_CODES.S_V2))
        assertEquals(expected, StoragePermissions.requiredReadPermissions(Build.VERSION_CODES.N))
    }

    @Test
    fun `never requests all-files or write access`() {
        for (sdk in 24..35) {
            val perms = StoragePermissions.requiredReadPermissions(sdk)
            assert(Manifest.permission.MANAGE_EXTERNAL_STORAGE !in perms)
            assert(Manifest.permission.WRITE_EXTERNAL_STORAGE !in perms)
        }
    }
}
