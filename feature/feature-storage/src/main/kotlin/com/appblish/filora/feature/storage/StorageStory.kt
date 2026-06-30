package com.appblish.filora.feature.storage

import com.appblish.filora.core.domain.model.MediaCategory
import com.appblish.filora.core.domain.model.StorageBreakdown

/**
 * Pure, presentation-ready model for the **storage-story** hero band (hi-fi spec §3.5,
 * the Direction-C borrowing in A-blend).
 *
 * It is derived **entirely from the already-computed [StorageBreakdown]** — no second
 * scan (principle #3: speed is a design constraint). The hero band renders a calm
 * year-in-review: total free space, a segmented by-category bar, and one honest
 * motivating line. There is no historical "freed this month" data in v1, so the story
 * is deliberately limited to facts the breakdown already proves ([StorageBreakdown]'s
 * doc-comment: never claim categorized bytes equal used bytes).
 */
data class StorageStory(
    val freeBytes: Long,
    val totalBytes: Long,
    val usedBytes: Long,
    /** Categorized slices, largest first, with their fraction of [totalBytes]. */
    val slices: List<StorageStorySlice>,
    /** Used bytes the media index can't attribute to a category (a non-negative remainder). */
    val uncategorizedBytes: Long,
) {
    /** True once there is a real volume to talk about; gates the count-up + any stat. */
    val hasData: Boolean get() = totalBytes > 0L

    /** Used fraction of total, clamped — drives the bar's filled width. */
    val usedFraction: Float
        get() = if (totalBytes > 0L) (usedBytes.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f

    /** The single largest category, used for the motivating line; null when nothing is indexed. */
    val topCategory: MediaCategory? get() = slices.firstOrNull()?.category

    companion object {
        /**
         * Aggregates every volume in [breakdown] into one story. Free/used/total are
         * summed across volumes; category sizes are summed across the (indexed) volumes
         * that carry them. Zero-size categories are dropped so the bar stays honest.
         */
        fun from(breakdown: StorageBreakdown?): StorageStory {
            val volumes = breakdown?.volumes.orEmpty()
            val totalBytes = volumes.sumOf { it.volume.totalBytes }
            val freeBytes = volumes.sumOf { it.volume.availableBytes }
            val usedBytes = volumes.sumOf { it.volume.usedBytes }

            val byCategory = linkedMapOf<MediaCategory, Long>()
            volumes.forEach { v ->
                v.categories.forEach { usage ->
                    if (usage.sizeBytes > 0L) {
                        byCategory[usage.category] = (byCategory[usage.category] ?: 0L) + usage.sizeBytes
                    }
                }
            }

            val slices =
                byCategory.entries
                    .sortedByDescending { it.value }
                    .map { (category, bytes) ->
                        StorageStorySlice(
                            category = category,
                            sizeBytes = bytes,
                            fraction = if (totalBytes > 0L) (bytes.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f,
                        )
                    }

            val categorizedBytes = slices.sumOf { it.sizeBytes }
            val uncategorizedBytes = (usedBytes - categorizedBytes).coerceAtLeast(0L)

            return StorageStory(
                freeBytes = freeBytes,
                totalBytes = totalBytes,
                usedBytes = usedBytes,
                slices = slices,
                uncategorizedBytes = uncategorizedBytes,
            )
        }
    }
}

/** One category's contribution to the storage-story bar. */
data class StorageStorySlice(
    val category: MediaCategory,
    val sizeBytes: Long,
    /** Share of [StorageStory.totalBytes] in `0f..1f`. */
    val fraction: Float,
)
