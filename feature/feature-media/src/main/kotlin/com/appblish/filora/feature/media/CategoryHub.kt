package com.appblish.filora.feature.media

import androidx.annotation.StringRes
import com.appblish.filora.core.domain.model.MediaCategory

/**
 * The seven first-class category hubs surfaced on the Media screen (FR-6.1), in
 * display order: Images, Video, Audio, Docs, Downloads, APKs, Archives. Each maps
 * to a domain [MediaCategory] whose count comes from `MediaRepository.categoryCounts`.
 *
 * [labelRes] is the string resource for the hub's user-facing label, resolved at the
 * Composable render site (NFR-7).
 *
 * [MediaCategory.Other] is intentionally absent — it is a classifier fallback
 * bucket, not a user-facing hub.
 */
enum class CategoryHub(
    val category: MediaCategory,
    @StringRes val labelRes: Int,
) {
    Images(MediaCategory.Images, R.string.media_hub_images),
    Video(MediaCategory.Video, R.string.media_hub_video),
    Audio(MediaCategory.Audio, R.string.media_hub_audio),
    Docs(MediaCategory.Documents, R.string.media_hub_docs),
    Downloads(MediaCategory.Downloads, R.string.media_hub_downloads),
    Apks(MediaCategory.Apps, R.string.media_hub_apks),
    Archives(MediaCategory.Archives, R.string.media_hub_archives),
    ;

    companion object {
        /** Hubs in FR-6.1 display order. */
        val ordered: List<CategoryHub> = entries.toList()
    }
}

/**
 * View data for one hub tile: the [hub] and its resolved item [count]. The label and
 * count caption are string resources resolved at the Composable render site (NFR-7).
 */
data class CategoryHubTile(
    val hub: CategoryHub,
    val count: Int,
)

/**
 * Builds the ordered seven-hub tile list from category [counts]. Categories absent
 * from the map (or with a negative count) resolve to 0, so the full grid always
 * renders even when MediaStore returned a partial map or failed entirely.
 */
fun buildHubTiles(counts: Map<MediaCategory, Int>): List<CategoryHubTile> =
    CategoryHub.ordered.map { hub ->
        CategoryHubTile(hub, counts[hub.category]?.coerceAtLeast(0) ?: 0)
    }
