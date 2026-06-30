package com.appblish.filora.core.data.thumbnail

import android.graphics.Bitmap

/**
 * Bitmap-typed view over [BoundedLruCache], sizing each entry by its actual
 * allocation so the byte budget reflects real heap pressure. Provided as a Hilt
 * singleton (see ThumbnailModule), giving the application one shared cache that
 * outlives individual screens — thumbnails stay warm across navigation (FR-6.2).
 *
 * Evicted bitmaps are intentionally not recycled here: a composable may still be
 * drawing one after it leaves the cache, and recycling underneath it would crash.
 * Dropping the reference lets the GC reclaim it once the UI releases it.
 */
class ThumbnailCache(
    maxSizeBytes: Long
) {
    private val cache = BoundedLruCache<ThumbnailKey, Bitmap>(maxSizeBytes) { it.allocationByteCount }

    /** Current footprint of cached bitmaps, in bytes. */
    val sizeBytes: Long get() = cache.sizeBytes

    /** Number of bitmaps currently cached. */
    val count: Int get() = cache.count

    fun get(key: ThumbnailKey): Bitmap? = cache.get(key)

    fun put(
        key: ThumbnailKey,
        bitmap: Bitmap,
    ) {
        cache.put(key, bitmap)
    }

    /** Empties the cache, e.g. on a low-memory callback. */
    fun clear() {
        cache.clear()
    }
}
