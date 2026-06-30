package com.appblish.filora.core.domain.model

/**
 * Per-volume storage usage broken down by media category (FR-8.1).
 *
 * Each [VolumeBreakdown] pairs a [StorageVolume]'s used/free figures with the
 * by-category slices for the media indexed on that volume. MediaStore indexes the
 * primary shared volume, so only the primary volume carries category slices;
 * removable volumes still report used/free but an empty [VolumeBreakdown.categories]
 * (their content isn't in the media index). The breakdown is therefore honest about
 * what can be attributed: [VolumeBreakdown.categorizedBytes] is a lower bound on
 * usage, never claimed to equal [StorageVolume.usedBytes].
 */
data class StorageBreakdown(
    val volumes: List<VolumeBreakdown>,
)

/** One volume's used/free plus its by-category slices (empty when not indexed). */
data class VolumeBreakdown(
    val volume: StorageVolume,
    val categories: List<CategoryUsage>,
) {
    /** Bytes attributed to indexed categories; a subset of [StorageVolume.usedBytes]. */
    val categorizedBytes: Long get() = categories.sumOf { it.sizeBytes }
}

/** Bytes and item count occupied by a single [MediaCategory] on a volume. */
data class CategoryUsage(
    val category: MediaCategory,
    val sizeBytes: Long,
    val itemCount: Int,
)
