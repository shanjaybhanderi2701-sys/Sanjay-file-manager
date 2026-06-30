package com.appblish.filora.feature.storage

import com.appblish.filora.core.domain.model.CategoryUsage
import com.appblish.filora.core.domain.model.MediaCategory
import com.appblish.filora.core.domain.model.StorageBreakdown
import com.appblish.filora.core.domain.model.StorageVolume
import com.appblish.filora.core.domain.model.VolumeBreakdown
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class StorageStoryTest {
    private fun volume(
        id: String,
        total: Long,
        available: Long,
        removable: Boolean = false,
    ) = StorageVolume(
        id = id,
        label = id,
        rootPath = "/$id",
        totalBytes = total,
        availableBytes = available,
        isRemovable = removable,
        isPrimary = id == "primary",
    )

    @Test
    fun `null breakdown yields an empty, dataless story`() {
        val story = StorageStory.from(null)

        assertThat(story.hasData).isFalse()
        assertThat(story.totalBytes).isEqualTo(0L)
        assertThat(story.freeBytes).isEqualTo(0L)
        assertThat(story.slices).isEmpty()
        assertThat(story.topCategory).isNull()
        assertThat(story.usedFraction).isEqualTo(0f)
    }

    @Test
    fun `single volume sums free, used and the uncategorized remainder`() {
        val breakdown =
            StorageBreakdown(
                volumes =
                    listOf(
                        VolumeBreakdown(
                            volume = volume("primary", total = 100, available = 40),
                            categories =
                                listOf(
                                    CategoryUsage(MediaCategory.Images, sizeBytes = 30, itemCount = 3),
                                    CategoryUsage(MediaCategory.Video, sizeBytes = 10, itemCount = 1),
                                ),
                        ),
                    ),
            )

        val story = StorageStory.from(breakdown)

        assertThat(story.hasData).isTrue()
        assertThat(story.freeBytes).isEqualTo(40L)
        assertThat(story.usedBytes).isEqualTo(60L)
        assertThat(story.totalBytes).isEqualTo(100L)
        // used 60 - categorized 40 = 20 unattributed bytes.
        assertThat(story.uncategorizedBytes).isEqualTo(20L)
        assertThat(story.usedFraction).isWithin(1e-4f).of(0.6f)
    }

    @Test
    fun `slices are largest-first with fraction of total`() {
        val breakdown =
            StorageBreakdown(
                volumes =
                    listOf(
                        VolumeBreakdown(
                            volume = volume("primary", total = 200, available = 100),
                            categories =
                                listOf(
                                    CategoryUsage(MediaCategory.Audio, sizeBytes = 20, itemCount = 2),
                                    CategoryUsage(MediaCategory.Images, sizeBytes = 60, itemCount = 6),
                                ),
                        ),
                    ),
            )

        val story = StorageStory.from(breakdown)

        assertThat(story.slices.map { it.category })
            .containsExactly(MediaCategory.Images, MediaCategory.Audio)
            .inOrder()
        assertThat(story.topCategory).isEqualTo(MediaCategory.Images)
        assertThat(story.slices.first().fraction).isWithin(1e-4f).of(0.3f) // 60 / 200
    }

    @Test
    fun `category sizes aggregate across volumes and zero slices are dropped`() {
        val breakdown =
            StorageBreakdown(
                volumes =
                    listOf(
                        VolumeBreakdown(
                            volume = volume("primary", total = 100, available = 50),
                            categories =
                                listOf(
                                    CategoryUsage(MediaCategory.Images, sizeBytes = 10, itemCount = 1),
                                    CategoryUsage(MediaCategory.Other, sizeBytes = 0, itemCount = 0),
                                ),
                        ),
                        VolumeBreakdown(
                            volume = volume("sd", total = 100, available = 80, removable = true),
                            categories =
                                listOf(
                                    CategoryUsage(MediaCategory.Images, sizeBytes = 15, itemCount = 2),
                                ),
                        ),
                    ),
            )

        val story = StorageStory.from(breakdown)

        assertThat(story.totalBytes).isEqualTo(200L)
        assertThat(story.freeBytes).isEqualTo(130L)
        // Images: 10 + 15 = 25, summed across both volumes; the zero-byte Other is dropped.
        assertThat(story.slices).hasSize(1)
        assertThat(story.slices.first().category).isEqualTo(MediaCategory.Images)
        assertThat(story.slices.first().sizeBytes).isEqualTo(25L)
    }
}
