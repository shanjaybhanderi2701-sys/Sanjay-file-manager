package com.appblish.filora.core.data.thumbnail

import com.appblish.filora.core.domain.model.FileItem
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ThumbnailRequestTest {
    private fun item(
        path: String = "content://media/external/file/1",
        mime: String? = "image/jpeg",
        modified: Long = 1_000L,
    ) = FileItem(
        name = "photo.jpg",
        path = path,
        isDirectory = false,
        sizeBytes = 4096,
        lastModifiedEpochMillis = modified,
        mimeType = mime,
    )

    @Test
    fun `factory copies locator, mime and timestamp from the file item`() {
        val request = ThumbnailRequest.of(item(), targetWidthPx = 200, targetHeightPx = 150)

        assertThat(request.sourcePath).isEqualTo("content://media/external/file/1")
        assertThat(request.mimeType).isEqualTo("image/jpeg")
        assertThat(request.targetWidthPx).isEqualTo(200)
        assertThat(request.targetHeightPx).isEqualTo(150)
        assertThat(request.lastModifiedEpochMillis).isEqualTo(1_000L)
    }

    @Test
    fun `images and videos are thumbnailable, other types are not`() {
        assertThat(ThumbnailRequest.of(item(mime = "image/png"), 100, 100).isThumbnailable).isTrue()
        assertThat(ThumbnailRequest.of(item(mime = "video/mp4"), 100, 100).isThumbnailable).isTrue()
        assertThat(ThumbnailRequest.of(item(mime = "application/pdf"), 100, 100).isThumbnailable).isFalse()
        assertThat(ThumbnailRequest.of(item(mime = null), 100, 100).isThumbnailable).isFalse()
    }

    @Test
    fun `a changed last-modified stamp yields a different cache key`() {
        val before = ThumbnailRequest.of(item(modified = 1_000L), 200, 200).key
        val after = ThumbnailRequest.of(item(modified = 2_000L), 200, 200).key

        assertThat(after).isNotEqualTo(before)
    }

    @Test
    fun `same source at different sizes yields different cache keys`() {
        val small = ThumbnailRequest.of(item(), 100, 100).key
        val large = ThumbnailRequest.of(item(), 400, 400).key

        assertThat(small).isNotEqualTo(large)
    }

    @Test
    fun `identical inputs produce equal keys so the cache hits`() {
        val a = ThumbnailRequest.of(item(), 200, 200).key
        val b = ThumbnailRequest.of(item(), 200, 200).key

        assertThat(a).isEqualTo(b)
    }
}
