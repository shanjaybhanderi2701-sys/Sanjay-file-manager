package com.appblish.filora.feature.media

import com.appblish.filora.core.domain.model.MediaCategory
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Pure hub ordering + count/caption mapping for the Media category hubs (FR-6.1). */
class CategoryHubTest {
    @Test
    fun `hubs are the seven FR-6_1 categories in display order`() {
        val categories = CategoryHub.ordered.map { it.category }

        assertThat(categories)
            .containsExactly(
                MediaCategory.Images,
                MediaCategory.Video,
                MediaCategory.Audio,
                MediaCategory.Documents,
                MediaCategory.Downloads,
                MediaCategory.Apps,
                MediaCategory.Archives,
            ).inOrder()
    }

    @Test
    fun `Other is not surfaced as a hub`() {
        val categories = CategoryHub.ordered.map { it.category }

        assertThat(categories).doesNotContain(MediaCategory.Other)
    }

    @Test
    fun `buildHubTiles always renders all seven hubs in order`() {
        val tiles = buildHubTiles(emptyMap())

        assertThat(tiles).hasSize(7)
        assertThat(tiles.map { it.hub }).isEqualTo(CategoryHub.ordered)
    }

    @Test
    fun `counts are looked up per category`() {
        val tiles =
            buildHubTiles(
                mapOf(
                    MediaCategory.Images to 12,
                    MediaCategory.Audio to 3,
                ),
            )
        val byHub = tiles.associateBy { it.hub }

        assertThat(byHub.getValue(CategoryHub.Images).count).isEqualTo(12)
        assertThat(byHub.getValue(CategoryHub.Audio).count).isEqualTo(3)
    }

    @Test
    fun `missing or negative counts resolve to zero`() {
        val tiles = buildHubTiles(mapOf(MediaCategory.Video to -5))
        val byHub = tiles.associateBy { it.hub }

        // absent from the map
        assertThat(byHub.getValue(CategoryHub.Docs).count).isEqualTo(0)
        // present but negative is clamped
        assertThat(byHub.getValue(CategoryHub.Video).count).isEqualTo(0)
    }

    @Test
    fun `caption pluralises around zero one and many`() {
        assertThat(CategoryHubTile(CategoryHub.Images, 0).caption).isEqualTo("Empty")
        assertThat(CategoryHubTile(CategoryHub.Images, 1).caption).isEqualTo("1 item")
        assertThat(CategoryHubTile(CategoryHub.Images, 42).caption).isEqualTo("42 items")
    }

    @Test
    fun `tile label mirrors the hub label`() {
        assertThat(CategoryHubTile(CategoryHub.Apks, 0).label).isEqualTo("APKs")
        assertThat(CategoryHubTile(CategoryHub.Docs, 0).label).isEqualTo("Docs")
    }
}
