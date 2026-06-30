package com.appblish.filora.core.ui.image

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest

/**
 * Provides the shared thumbnail [ImageLoader] down the Compose tree.
 *
 * Wire the Hilt-provided [Singleton][javax.inject.Singleton] loader at the app root:
 * ```
 * CompositionLocalProvider(LocalThumbnailImageLoader provides imageLoader) { … }
 * ```
 * When nothing is provided (e.g. `@Preview`), [MediaThumbnail] falls back to a
 * locally-built loader so it still renders, but that instance is *not* shared and
 * should never be relied on in production.
 */
val LocalThumbnailImageLoader = staticCompositionLocalOf<ImageLoader?> { null }

/**
 * Resolves the [ImageLoader] [MediaThumbnail] should use: the one published via
 * [LocalThumbnailImageLoader] when present, otherwise a remembered fallback so the
 * composable degrades gracefully outside a fully-wired graph.
 */
@Composable
fun rememberThumbnailImageLoader(): ImageLoader {
    LocalThumbnailImageLoader.current?.let { return it }
    val context = LocalContext.current
    return remember(context) { ThumbnailImageLoaders.create(context) }
}

/**
 * Renders a media thumbnail for [model] (a content `Uri`, path string, or any
 * Coil-supported data) using the shared, cache-backed [imageLoader] (FR-6.2).
 *
 * Decoding happens off the main thread; while a frame loads — or if it cannot be
 * decoded — a neutral [fallbackIcon] placeholder is shown so a scrolling grid
 * never blocks or crashes on a missing/corrupt item.
 */
@Composable
fun MediaThumbnail(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    imageLoader: ImageLoader = rememberThumbnailImageLoader(),
    contentScale: ContentScale = ContentScale.Crop,
    fallbackIcon: ImageVector = Icons.Outlined.Image,
) {
    val context = LocalContext.current
    val request =
        remember(model, context) {
            ImageRequest
                .Builder(context)
                .data(model)
                .crossfade(true)
                .build()
        }
    SubcomposeAsyncImage(
        model = request,
        imageLoader = imageLoader,
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier,
        loading = { ThumbnailPlaceholder(fallbackIcon) },
        error = { ThumbnailPlaceholder(fallbackIcon) },
    )
}

@Composable
private fun ThumbnailPlaceholder(icon: ImageVector) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
