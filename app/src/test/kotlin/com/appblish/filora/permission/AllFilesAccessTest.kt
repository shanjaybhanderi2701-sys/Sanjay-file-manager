package com.appblish.filora.permission

import android.os.Build
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies the all-files access opt-in gate (T022). Pure JVM test —
 * [AllFilesAccess.shouldOffer] is parameterised on the build flag and SDK level so
 * the policy ("only on the fullaccess build, only where MANAGE_EXTERNAL_STORAGE
 * exists") is checked without a device.
 */
class AllFilesAccessTest {
    @Test
    fun `offered on a full-access build running api 30+`() {
        assertTrue(
            AllFilesAccess.shouldOffer(fullAccessSupported = true, sdkInt = Build.VERSION_CODES.R),
        )
        assertTrue(
            AllFilesAccess.shouldOffer(
                fullAccessSupported = true,
                sdkInt = Build.VERSION_CODES.VANILLA_ICE_CREAM,
            ),
        )
    }

    @Test
    fun `never offered on the standard play-default build`() {
        for (sdk in 24..35) {
            assertFalse(AllFilesAccess.shouldOffer(fullAccessSupported = false, sdkInt = sdk))
        }
    }

    @Test
    fun `not offered below api 30 even on a full-access build`() {
        assertFalse(
            AllFilesAccess.shouldOffer(fullAccessSupported = true, sdkInt = Build.VERSION_CODES.P),
        )
        assertFalse(
            AllFilesAccess.shouldOffer(
                fullAccessSupported = true,
                sdkInt = Build.VERSION_CODES.Q,
            ),
        )
    }
}
