package com.appblish.filora.core.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SearchFilterTest {
    private fun file(
        name: String,
        size: Long = 1_000,
        modified: Long = 1_000,
        mime: String? = null,
        isDirectory: Boolean = false,
    ) = FileItem(
        name = name,
        path = "/root/$name",
        isDirectory = isDirectory,
        sizeBytes = size,
        lastModifiedEpochMillis = modified,
        mimeType = mime,
    )

    @Test
    fun `empty filter keeps everything including directories`() {
        val filter = SearchFilter()

        assertThat(filter.isEmpty).isTrue()
        assertThat(filter.matches(file("a.txt"))).isTrue()
        assertThat(filter.matches(file("docs", isDirectory = true))).isTrue()
    }

    @Test
    fun `type dimension ORs the selected types`() {
        val filter = SearchFilter(types = setOf(FileTypeFilter.Image, FileTypeFilter.Audio))

        assertThat(filter.matches(file("photo.jpg"))).isTrue()
        assertThat(filter.matches(file("song.mp3"))).isTrue()
        assertThat(filter.matches(file("clip.mp4"))).isFalse()
        assertThat(filter.matches(file("notes.pdf"))).isFalse()
    }

    @Test
    fun `type matches by mime when extension is absent`() {
        val filter = SearchFilter(types = setOf(FileTypeFilter.Image))

        assertThat(filter.matches(file("IMG_0001", mime = "image/jpeg"))).isTrue()
        assertThat(filter.matches(file("blob", mime = "application/octet-stream"))).isFalse()
    }

    @Test
    fun `apk and archive types are recognized`() {
        assertThat(SearchFilter(types = setOf(FileTypeFilter.Apk)).matches(file("app.apk"))).isTrue()
        assertThat(SearchFilter(types = setOf(FileTypeFilter.Archive)).matches(file("bundle.zip"))).isTrue()
        assertThat(SearchFilter(types = setOf(FileTypeFilter.Archive)).matches(file("app.apk"))).isFalse()
    }

    @Test
    fun `size range is inclusive on both bounds`() {
        val filter = SearchFilter(minSizeBytes = 100, maxSizeBytes = 200)

        assertThat(filter.matches(file("a", size = 99))).isFalse()
        assertThat(filter.matches(file("a", size = 100))).isTrue()
        assertThat(filter.matches(file("a", size = 200))).isTrue()
        assertThat(filter.matches(file("a", size = 201))).isFalse()
    }

    @Test
    fun `date range is inclusive on both bounds`() {
        val filter = SearchFilter(modifiedAfterEpochMillis = 100, modifiedBeforeEpochMillis = 200)

        assertThat(filter.matches(file("a", modified = 99))).isFalse()
        assertThat(filter.matches(file("a", modified = 100))).isTrue()
        assertThat(filter.matches(file("a", modified = 200))).isTrue()
        assertThat(filter.matches(file("a", modified = 201))).isFalse()
    }

    @Test
    fun `dimensions combine with AND`() {
        val filter =
            SearchFilter(
                types = setOf(FileTypeFilter.Image),
                minSizeBytes = 500,
                modifiedAfterEpochMillis = 1_000,
            )

        // Passes type + size + date.
        assertThat(filter.matches(file("a.jpg", size = 600, modified = 2_000))).isTrue()
        // Right type and date, too small.
        assertThat(filter.matches(file("a.jpg", size = 100, modified = 2_000))).isFalse()
        // Right size and date, wrong type.
        assertThat(filter.matches(file("a.mp3", size = 600, modified = 2_000))).isFalse()
        // Right type and size, too old.
        assertThat(filter.matches(file("a.jpg", size = 600, modified = 500))).isFalse()
    }

    @Test
    fun `any active dimension excludes directories`() {
        val filter = SearchFilter(types = setOf(FileTypeFilter.Image))

        assertThat(filter.matches(file("pics", isDirectory = true))).isFalse()
    }

    @Test
    fun `open-ended bounds constrain only one side`() {
        assertThat(SearchFilter(minSizeBytes = 100).matches(file("a", size = 1_000_000))).isTrue()
        assertThat(SearchFilter(maxSizeBytes = 100).matches(file("a", size = 50))).isTrue()
        assertThat(SearchFilter(maxSizeBytes = 100).matches(file("a", size = 500))).isFalse()
    }
}
