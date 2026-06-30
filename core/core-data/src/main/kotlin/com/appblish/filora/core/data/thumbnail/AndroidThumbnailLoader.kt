package com.appblish.filora.core.data.thumbnail

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Size
import androidx.core.net.toUri
import com.appblish.filora.core.common.dispatcher.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

/**
 * [ThumbnailLoader] over [ContentResolver]. On API 29+ it delegates to
 * [ContentResolver.loadThumbnail], which returns a correctly downsampled thumbnail
 * for both images and video. On older platforms it stream-decodes images with a
 * power-of-two [ThumbnailSampling] downsample; pre-Q video frames are not extracted
 * (the row falls back to a type icon).
 *
 * Every decode runs on [ioDispatcher] (NFR-1.4 off-main thumbnailing) and any
 * platform failure is swallowed to null so a missing or corrupt file never crashes
 * a scrolling grid.
 */
class AndroidThumbnailLoader
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val cache: ThumbnailCache,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : ThumbnailLoader {
        private val contentResolver: ContentResolver get() = context.contentResolver

        override fun cached(request: ThumbnailRequest): Bitmap? = cache.get(request.key)

        override suspend fun load(request: ThumbnailRequest): Bitmap? {
            if (!request.isThumbnailable) return null
            cache.get(request.key)?.let { return it }

            val bitmap =
                withContext(ioDispatcher) {
                    coroutineContext.ensureActive()
                    decode(request)
                } ?: return null

            cache.put(request.key, bitmap)
            return bitmap
        }

        private fun decode(request: ThumbnailRequest): Bitmap? {
            val uri = request.sourcePath.toUri()
            return runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentResolver.loadThumbnail(
                        uri,
                        Size(request.targetWidthPx, request.targetHeightPx),
                        null,
                    )
                } else if (request.isImage) {
                    decodeDownsampledImage(uri, request.targetWidthPx, request.targetHeightPx)
                } else {
                    null
                }
            }.getOrNull()
        }

        // Two-pass decode: read the bounds first so the full-size bitmap is never
        // allocated, then decode at the computed sample size. The stream is reopened
        // because BitmapFactory consumes it.
        private fun decodeDownsampledImage(
            uri: Uri,
            reqWidth: Int,
            reqHeight: Int,
        ): Bitmap? {
            val bounds =
                BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, bounds)
            } ?: throw IOException("Cannot open $uri")

            val options =
                BitmapFactory.Options().apply {
                    inSampleSize =
                        ThumbnailSampling.calculateInSampleSize(
                            bounds.outWidth,
                            bounds.outHeight,
                            reqWidth,
                            reqHeight,
                        )
                }
            return contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, options)
            }
        }
    }
