package com.appblish.filora.core.data.media

import com.appblish.filora.core.common.util.FileExtensions
import com.appblish.filora.core.domain.model.MediaCategory

/**
 * Maps a single MediaStore row to exactly one [MediaCategory].
 *
 * Pure, Android-free, and unit-tested so the bucketing rules can evolve without a
 * device. [AndroidMediaStoreSource] reads the cursor and delegates every row here;
 * the repository never re-classifies.
 *
 * Each file lands in a single bucket (no double counting). Resolution order:
 *  1. MediaStore's own [media type][android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE]
 *     for Image/Video/Audio — authoritative when the indexer set it.
 *  2. MIME type, then file extension, for Apps/Archives/Documents.
 *  3. A `Download/` location for anything still untyped.
 *  4. [MediaCategory.Other] as the fallback.
 */
object MediaClassifier {
    // Mirrors MediaStore.Files.FileColumns media-type constants without depending on
    // android.* so this file stays unit-testable on a plain JVM.
    const val MEDIA_TYPE_NONE = 0
    const val MEDIA_TYPE_IMAGE = 1
    const val MEDIA_TYPE_AUDIO = 2
    const val MEDIA_TYPE_VIDEO = 3

    private const val APK_EXTENSION = "apk"
    private const val APK_MIME = "application/vnd.android.package-archive"

    /**
     * @param mediaType the row's `MEDIA_TYPE`, or [MEDIA_TYPE_NONE] when absent.
     * @param mimeType the row's MIME type, if any.
     * @param displayName the file name, used for extension-based fallback.
     * @param relativePath the row's `RELATIVE_PATH`/data path, used to spot downloads.
     */
    fun classify(
        mediaType: Int,
        mimeType: String?,
        displayName: String,
        relativePath: String?,
    ): MediaCategory {
        val mime = mimeType?.lowercase()?.trim().orEmpty()
        return byMediaType(mediaType)
            ?: byMime(mime)
            ?: byNameOrMime(displayName, mime)
            ?: if (isInDownloads(relativePath)) MediaCategory.Downloads else MediaCategory.Other
    }

    private fun byMediaType(mediaType: Int): MediaCategory? =
        when (mediaType) {
            MEDIA_TYPE_IMAGE -> MediaCategory.Images
            MEDIA_TYPE_VIDEO -> MediaCategory.Video
            MEDIA_TYPE_AUDIO -> MediaCategory.Audio
            else -> null
        }

    private fun byMime(mime: String): MediaCategory? =
        when {
            mime.startsWith("image/") -> MediaCategory.Images
            mime.startsWith("video/") -> MediaCategory.Video
            mime.startsWith("audio/") -> MediaCategory.Audio
            else -> null
        }

    private fun byNameOrMime(
        displayName: String,
        mime: String,
    ): MediaCategory? =
        when {
            mime == APK_MIME || FileExtensions.extensionOf(displayName) == APK_EXTENSION -> MediaCategory.Apps
            FileExtensions.isArchive(displayName) -> MediaCategory.Archives
            FileExtensions.isDocument(displayName) || mime.startsWith("text/") -> MediaCategory.Documents
            else -> null
        }

    private fun isInDownloads(relativePath: String?): Boolean {
        val path = relativePath?.lowercase() ?: return false
        return path.contains("download/") ||
            path.contains("downloads/") ||
            path.endsWith("download") ||
            path.endsWith("downloads")
    }
}
