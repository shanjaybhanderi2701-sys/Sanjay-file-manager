package com.appblish.filora.core.data.media

import com.appblish.filora.core.domain.model.MediaCategory
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MediaClassifierTest {
    private fun classify(
        mediaType: Int = MediaClassifier.MEDIA_TYPE_NONE,
        mimeType: String? = null,
        displayName: String = "file",
        relativePath: String? = null,
    ) = MediaClassifier.classify(mediaType, mimeType, displayName, relativePath)

    @Test
    fun `MediaStore media type wins over everything`() {
        assertThat(classify(mediaType = MediaClassifier.MEDIA_TYPE_IMAGE, displayName = "x.pdf"))
            .isEqualTo(MediaCategory.Images)
        assertThat(classify(mediaType = MediaClassifier.MEDIA_TYPE_VIDEO)).isEqualTo(MediaCategory.Video)
        assertThat(classify(mediaType = MediaClassifier.MEDIA_TYPE_AUDIO)).isEqualTo(MediaCategory.Audio)
    }

    @Test
    fun `mime type prefix classifies media when media type is absent`() {
        assertThat(classify(mimeType = "image/png", displayName = "no-ext")).isEqualTo(MediaCategory.Images)
        assertThat(classify(mimeType = "video/mp4", displayName = "no-ext")).isEqualTo(MediaCategory.Video)
        assertThat(classify(mimeType = "audio/mpeg", displayName = "no-ext")).isEqualTo(MediaCategory.Audio)
    }

    @Test
    fun `apks are apps by mime or extension`() {
        assertThat(classify(mimeType = "application/vnd.android.package-archive", displayName = "a"))
            .isEqualTo(MediaCategory.Apps)
        assertThat(classify(displayName = "game.apk")).isEqualTo(MediaCategory.Apps)
    }

    @Test
    fun `archives and documents fall back to extension`() {
        assertThat(classify(displayName = "backup.zip")).isEqualTo(MediaCategory.Archives)
        assertThat(classify(displayName = "report.pdf")).isEqualTo(MediaCategory.Documents)
        assertThat(classify(mimeType = "text/plain", displayName = "notes")).isEqualTo(MediaCategory.Documents)
    }

    @Test
    fun `untyped files in a download folder are Downloads`() {
        assertThat(classify(displayName = "blob", relativePath = "Download/")).isEqualTo(MediaCategory.Downloads)
        assertThat(classify(displayName = "blob", relativePath = "Download/sub/"))
            .isEqualTo(MediaCategory.Downloads)
    }

    @Test
    fun `a typed file in downloads keeps its type bucket`() {
        // Documents takes priority over location, so a downloaded PDF is a Document.
        assertThat(classify(displayName = "manual.pdf", relativePath = "Download/")).isEqualTo(MediaCategory.Documents)
    }

    @Test
    fun `unknown files fall through to Other`() {
        assertThat(classify(displayName = "mystery.xyz", relativePath = "Documents/")).isEqualTo(MediaCategory.Other)
        assertThat(classify(displayName = "no-extension")).isEqualTo(MediaCategory.Other)
    }
}
