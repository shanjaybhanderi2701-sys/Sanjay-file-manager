package com.appblish.filora.feature.media.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.appblish.filora.core.ui.image.MediaThumbnail
import com.appblish.filora.feature.media.R

/**
 * Full-screen, in-app image preview with pinch-to-zoom, drag-to-pan, and double-tap
 * toggle (FR-6.1). The bitmap is decoded off the main thread through the shared,
 * cache-backed Coil loader behind [MediaThumbnail] (FR-6.2), so opening a preview
 * reuses the thumbnail already warmed by the category grid.
 *
 * All gesture clamping is delegated to the pure [ImageZoomMath] so the picture can
 * never be zoomed past its bounds or dragged off-screen; this composable only owns
 * the viewport size and the transient [ImageTransform] state.
 *
 * [model] is the image's `content://` URI (or any Coil-supported locator). [onClose]
 * dismisses the preview; [onOpenExternally] hands the same item to the system viewer
 * for formats this in-app preview can't render well (e.g. animated/vector assets).
 */
@Composable
fun ImagePreviewScreen(
    model: Any?,
    onClose: () -> Unit,
    onOpenExternally: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var transform by remember(model) { mutableStateOf(ImageTransform()) }
    var viewportWidth by remember { mutableStateOf(0f) }
    var viewportHeight by remember { mutableStateOf(0f) }

    val transformableState =
        rememberTransformableState { zoomChange, panChange, _ ->
            transform =
                ImageZoomMath.apply(
                    current = transform,
                    zoomChange = zoomChange,
                    panX = panChange.x,
                    panY = panChange.y,
                    viewportWidth = viewportWidth,
                    viewportHeight = viewportHeight,
                )
        }

    val previewLabel = stringResource(R.string.media_preview_image)

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(Color.Black)
                .onSizeChanged {
                    viewportWidth = it.width.toFloat()
                    viewportHeight = it.height.toFloat()
                },
        contentAlignment = Alignment.Center,
    ) {
        MediaThumbnail(
            model = model,
            contentDescription = previewLabel,
            contentScale = ContentScale.Fit,
            modifier =
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = transform.scale
                        scaleY = transform.scale
                        translationX = transform.offsetX
                        translationY = transform.offsetY
                    }.transformable(state = transformableState)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = { transform = ImageZoomMath.toggleDoubleTap(transform) },
                        )
                    },
        )

        IconButton(
            onClick = onClose,
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = stringResource(R.string.media_preview_close),
                tint = Color.White,
            )
        }

        IconButton(
            onClick = onOpenExternally,
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.OpenInNew,
                contentDescription = stringResource(R.string.media_preview_open_external),
                tint = Color.White,
            )
        }
    }
}
