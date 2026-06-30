package com.appblish.filora.core.data.thumbnail

import com.appblish.filora.core.domain.model.FileItem

/**
 * Identity of a cached thumbnail. Includes the target dimensions (the same file
 * rendered at two sizes is two cache entries) and the source's last-modified stamp,
 * so editing a file invalidates its stale thumbnail by producing a fresh key rather
 * than serving the old bitmap.
 */
data class ThumbnailKey(
    val sourcePath: String,
    val targetWidthPx: Int,
    val targetHeightPx: Int,
    val lastModifiedEpochMillis: Long,
)

/**
 * A request to load a thumbnail for one media item. [sourcePath] is the opaque
 * locator from [FileItem] (a `content://` URI for MediaStore items); [mimeType]
 * lets the loader pick an image-vs-video decode path on older platforms.
 */
data class ThumbnailRequest(
    val sourcePath: String,
    val mimeType: String?,
    val targetWidthPx: Int,
    val targetHeightPx: Int,
    val lastModifiedEpochMillis: Long,
) {
    val key: ThumbnailKey
        get() = ThumbnailKey(sourcePath, targetWidthPx, targetHeightPx, lastModifiedEpochMillis)

    val isImage: Boolean get() = mimeType?.startsWith("image/") == true
    val isVideo: Boolean get() = mimeType?.startsWith("video/") == true

    /** Only image and video items have meaningful pixel thumbnails (FR-6.2). */
    val isThumbnailable: Boolean get() = isImage || isVideo

    companion object {
        /** Builds a request for [item] at a square-ish [targetWidthPx] x [targetHeightPx] cell. */
        fun of(
            item: FileItem,
            targetWidthPx: Int,
            targetHeightPx: Int,
        ): ThumbnailRequest =
            ThumbnailRequest(
                sourcePath = item.path,
                mimeType = item.mimeType,
                targetWidthPx = targetWidthPx,
                targetHeightPx = targetHeightPx,
                lastModifiedEpochMillis = item.lastModifiedEpochMillis,
            )
    }
}
