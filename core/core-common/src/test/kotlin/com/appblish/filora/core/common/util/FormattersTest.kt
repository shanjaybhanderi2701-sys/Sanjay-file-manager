package com.appblish.filora.core.common.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.Locale

class FormattersTest {
    @Test
    fun `formatSize renders bytes`() {
        assertThat(Formatters.formatSize(0)).isEqualTo("0 B")
        assertThat(Formatters.formatSize(512)).isEqualTo("512 B")
    }

    @Test
    fun `formatSize renders binary units`() {
        assertThat(Formatters.formatSize(1536, Locale.US)).isEqualTo("1.5 KB")
        assertThat(Formatters.formatSize(1_048_576, Locale.US)).isEqualTo("1.0 MB")
    }

    @Test
    fun `formatSize clamps negatives to zero`() {
        assertThat(Formatters.formatSize(-5)).isEqualTo("0 B")
    }

    @Test
    fun `pathSegments collapses internal storage prefix`() {
        assertThat(Formatters.pathSegments("/storage/emulated/0/DCIM/Camera"))
            .containsExactly("Internal storage", "DCIM", "Camera")
            .inOrder()
    }

    @Test
    fun `extensionOf and baseName split names`() {
        assertThat(FileExtensions.extensionOf("photo.JPG")).isEqualTo("jpg")
        assertThat(FileExtensions.extensionOf("README")).isEqualTo("")
        assertThat(FileExtensions.baseName("archive.tar.gz")).isEqualTo("archive.tar")
        assertThat(FileExtensions.isArchive("backup.zip")).isTrue()
    }
}
