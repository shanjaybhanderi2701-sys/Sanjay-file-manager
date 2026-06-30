package com.appblish.filora.core.ui.image

import android.content.Context
import coil.ImageLoader
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy

/**
 * Builds the tuned [ImageLoader] that backs every [MediaThumbnail] (FR-6.2).
 *
 * The loader is intended to be held as a process-wide singleton (see
 * `ThumbnailImageLoaderModule`) so its in-memory and on-disk caches survive
 * navigation between category hubs and detail grids. Coil decodes off the main
 * thread by default, so scrolling stays smooth.
 *
 * Caches are bounded so the loader scales with device RAM and never grows the
 * cache directory without limit:
 *  - memory: a fraction of the available heap ([MEMORY_CACHE_HEAP_PERCENT]),
 *  - disk: capped at [DISK_CACHE_MAX_BYTES] under the app cache dir.
 *
 * A [VideoFrameDecoder] is registered so video URIs render a representative
 * frame instead of failing to decode.
 */
object ThumbnailImageLoaders {
    /** Share of the app heap the in-memory thumbnail cache may use. */
    private const val MEMORY_CACHE_HEAP_PERCENT = 0.25

    /** Hard ceiling on the on-disk thumbnail cache. */
    private const val DISK_CACHE_MAX_BYTES = 256L * 1024 * 1024

    /** Sub-directory of the app cache dir that holds decoded thumbnails. */
    private const val DISK_CACHE_DIR = "media_thumbnails"

    fun create(context: Context): ImageLoader {
        val appContext = context.applicationContext
        return ImageLoader
            .Builder(appContext)
            .components { add(VideoFrameDecoder.Factory()) }
            .memoryCachePolicy(CachePolicy.ENABLED)
            .memoryCache {
                MemoryCache
                    .Builder(appContext)
                    .maxSizePercent(MEMORY_CACHE_HEAP_PERCENT)
                    .build()
            }.diskCachePolicy(CachePolicy.ENABLED)
            .diskCache {
                DiskCache
                    .Builder()
                    .directory(appContext.cacheDir.resolve(DISK_CACHE_DIR))
                    .maxSizeBytes(DISK_CACHE_MAX_BYTES)
                    .build()
            }.build()
    }
}
