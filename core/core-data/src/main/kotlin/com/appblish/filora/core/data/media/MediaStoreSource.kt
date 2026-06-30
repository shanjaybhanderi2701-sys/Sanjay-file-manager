package com.appblish.filora.core.data.media

import com.appblish.filora.core.domain.model.MediaCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * A single media entry as read from the platform index, before it is mapped to a
 * domain [com.appblish.filora.core.domain.model.FileItem].
 *
 * [contentUri] is the stable locator handed to the domain layer (works under
 * scoped storage); [filePath] is the legacy `_data` column, kept only as a
 * best-effort display path on older devices.
 */
data class RawMediaEntry(
    val contentUri: String,
    val displayName: String,
    val mimeType: String?,
    val mediaType: Int,
    val sizeBytes: Long,
    val dateModifiedEpochMillis: Long,
    val relativePath: String?,
    val filePath: String?,
) {
    val category: MediaCategory
        get() = MediaClassifier.classify(mediaType, mimeType, displayName, relativePath)
}

/**
 * Reads the device's media index (MediaStore). The Android-specific cursor work
 * lives behind this interface so the repository's mapping/aggregation logic can be
 * unit-tested with a fake — the same seam used by
 * [com.appblish.filora.core.data.storage.VolumeEnumerator].
 */
interface MediaStoreSource {
    /** Item counts per category across the external media collection. */
    fun countByCategory(): Map<MediaCategory, Int>

    /** Total bytes per category across the external media collection. */
    fun sizeByCategory(): Map<MediaCategory, Long>

    /** All entries belonging to [category], for category browsing. */
    fun entriesIn(category: MediaCategory): List<RawMediaEntry>

    /**
     * Emits once each time the underlying media collection changes (a file is
     * added, removed, or rewritten), so callers can re-query for a live category
     * view (FR-6.1). The default is an empty flow, so sources that don't observe
     * — such as test fakes — fall back to a single one-shot read.
     */
    fun changes(): Flow<Unit> = emptyFlow()
}
