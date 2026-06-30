package com.appblish.filora.feature.media

import com.appblish.filora.core.domain.model.MediaCategory

/**
 * The seven first-class category hubs surfaced on the Media screen (FR-6.1), in
 * display order: Images, Video, Audio, Docs, Downloads, APKs, Archives. Each maps
 * to a domain [MediaCategory] whose count comes from `MediaRepository.categoryCounts`.
 *
 * [MediaCategory.Other] is intentionally absent — it is a classifier fallback
 * bucket, not a user-facing hub.
 */
enum class CategoryHub(
    val category: MediaCategory,
    val label: String,
) {
    Images(MediaCategory.Images, "Images"),
    Video(MediaCategory.Video, "Video"),
    Audio(MediaCategory.Audio, "Audio"),
    Docs(MediaCategory.Documents, "Docs"),
    Downloads(MediaCategory.Downloads, "Downloads"),
    Apks(MediaCategory.Apps, "APKs"),
    Archives(MediaCategory.Archives, "Archives"),
    ;

    companion object {
        /** Hubs in FR-6.1 display order. */
        val ordered: List<CategoryHub> = entries.toList()
    }
}

/**
 * View data for one hub tile: the [hub] and its resolved item [count]. [caption] is
 * the human-readable count line rendered under the tile label.
 */
data class CategoryHubTile(
    val hub: CategoryHub,
    val count: Int,
) {
    val label: String get() = hub.label

    val caption: String
        get() =
            when (count) {
                0 -> "Empty"
                1 -> "1 item"
                else -> "$count items"
            }
}

/**
 * Builds the ordered seven-hub tile list from category [counts]. Categories absent
 * from the map (or with a negative count) resolve to 0, so the full grid always
 * renders even when MediaStore returned a partial map or failed entirely.
 */
fun buildHubTiles(counts: Map<MediaCategory, Int>): List<CategoryHubTile> =
    CategoryHub.ordered.map { hub ->
        CategoryHubTile(hub, counts[hub.category]?.coerceAtLeast(0) ?: 0)
    }
