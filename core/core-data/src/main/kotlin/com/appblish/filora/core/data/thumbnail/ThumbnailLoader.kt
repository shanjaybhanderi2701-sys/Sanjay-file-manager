package com.appblish.filora.core.data.thumbnail

import android.graphics.Bitmap

/**
 * Loads downsampled image/video thumbnails, backed by a bounded in-memory cache.
 *
 * The two-method shape is what keeps scrolling smooth (FR-6.2 / NFR-9.2 "no scroll
 * block"): a row composable calls [cached] synchronously during layout for an
 * instant hit, and only falls back to the suspending [load] — which decodes off the
 * main thread — when the cache misses. Cancelling the caller's coroutine (the item
 * scrolled away) cancels the decode.
 */
interface ThumbnailLoader {
    /** Already-decoded bitmap for [request], or null. Never touches disk or decodes. */
    fun cached(request: ThumbnailRequest): Bitmap?

    /**
     * Returns a thumbnail for [request], decoding off the main thread on a cache
     * miss and storing the result. Returns null for non-thumbnailable items or when
     * decoding fails (missing file, unsupported codec) — callers fall back to an icon.
     */
    suspend fun load(request: ThumbnailRequest): Bitmap?
}
