package com.appblish.filora.feature.settings

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for the About screen's data (M13 T139). The composable itself is
 * exercised on-device, but the shipped open-source credits are pure data, so the
 * contract that matters — every entry names a library and a non-blank license, and
 * the list has no accidental duplicates — is asserted here without a launcher.
 */
class AboutScreenTest {
    @Test
    fun `oss licenses are all named and licensed`() {
        val licenses = filoraOssLicenses()

        assertThat(licenses).isNotEmpty()
        licenses.forEach { entry ->
            assertThat(entry.name).isNotEmpty()
            assertThat(entry.license).isNotEmpty()
        }
    }

    @Test
    fun `oss license entries are unique by name`() {
        val names = filoraOssLicenses().map { it.name }

        assertThat(names).containsNoDuplicates()
    }
}
