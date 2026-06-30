package com.appblish.filora.permission

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Verifies the app-details settings deep-link data part (T028). Pure JVM test —
 * [AppSettings.packageUri] is the source of truth for the `package:`-scheme URI the
 * real [AppSettings.appDetailsIntent] parses, so checking the string needs no
 * Android runtime.
 */
class AppSettingsTest {
    @Test
    fun `package uri uses the package scheme with the app id`() {
        assertEquals(
            "package:com.appblish.filora",
            AppSettings.packageUri("com.appblish.filora"),
        )
    }

    @Test
    fun `package uri reflects a fullaccess application id suffix`() {
        assertEquals(
            "package:com.appblish.filora.fullaccess",
            AppSettings.packageUri("com.appblish.filora.fullaccess"),
        )
    }
}
