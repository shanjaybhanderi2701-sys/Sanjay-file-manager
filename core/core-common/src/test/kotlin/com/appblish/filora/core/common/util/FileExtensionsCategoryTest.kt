package com.appblish.filora.core.common.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FileExtensionsCategoryTest {
    @Test
    fun `classifies each known type by extension`() {
        assertThat(FileExtensions.categoryOf("vacation.JPG")).isEqualTo(FileCategory.Image)
        assertThat(FileExtensions.categoryOf("clip.mp4")).isEqualTo(FileCategory.Video)
        assertThat(FileExtensions.categoryOf("track.flac")).isEqualTo(FileCategory.Audio)
        assertThat(FileExtensions.categoryOf("report.pdf")).isEqualTo(FileCategory.Document)
        assertThat(FileExtensions.categoryOf("backup.tar.gz")).isEqualTo(FileCategory.Archive)
        assertThat(FileExtensions.categoryOf("Filora-release.apk")).isEqualTo(FileCategory.Apk)
    }

    @Test
    fun `is case-insensitive on the extension`() {
        assertThat(FileExtensions.categoryOf("NOTES.MD")).isEqualTo(FileCategory.Document)
        assertThat(FileExtensions.categoryOf("song.M4A")).isEqualTo(FileCategory.Audio)
    }

    @Test
    fun `unknown or extension-less names are Other`() {
        assertThat(FileExtensions.categoryOf("mystery.xyz")).isEqualTo(FileCategory.Other)
        assertThat(FileExtensions.categoryOf("README")).isEqualTo(FileCategory.Other)
        assertThat(FileExtensions.categoryOf(".gitignore")).isEqualTo(FileCategory.Other)
    }
}
