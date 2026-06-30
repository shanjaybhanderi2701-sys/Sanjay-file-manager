package com.appblish.filora.feature.media.preview

/**
 * Immutable zoom/pan transform applied to the image preview (FR-6.1). Expressed in
 * platform-free primitives — a scale factor and a translation in pixels from the
 * centred, fit-to-viewport position — so the clamping rules stay unit-testable
 * without an emulator. The Compose layer feeds it gesture deltas and renders the
 * result through a `graphicsLayer`.
 */
data class ImageTransform(
    val scale: Float = MIN_SCALE,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
) {
    /** True once the user has zoomed past the fit-to-viewport baseline. */
    val isZoomed: Boolean get() = scale > MIN_SCALE + EPSILON

    companion object {
        const val MIN_SCALE = 1f
        const val MAX_SCALE = 5f

        /** Scale a double-tap zooms to (and from which it resets back to [MIN_SCALE]). */
        const val DOUBLE_TAP_SCALE = 2.5f

        internal const val EPSILON = 0.001f
    }
}

/**
 * Pure zoom/pan math for the image preview. Keeping the gesture rules here — rather
 * than inline in the composable — means pinch clamping, edge-bounded panning, and the
 * double-tap toggle can all be verified in plain JVM tests (FR-6.1, FR-6.2).
 */
object ImageZoomMath {
    /** Clamps a raw scale into the allowed [min]‥[max] zoom range. */
    fun clampScale(
        scale: Float,
        min: Float = ImageTransform.MIN_SCALE,
        max: Float = ImageTransform.MAX_SCALE,
    ): Float = scale.coerceIn(min, max)

    /**
     * How far, in pixels, the content may pan from centre on one axis at [scale]: half
     * the overflow once the [viewportSize]-wide image is enlarged. Zero (no panning)
     * while the image still fits, so the picture can never be dragged off-screen.
     */
    fun maxPan(
        viewportSize: Float,
        scale: Float,
    ): Float = (viewportSize * (scale - ImageTransform.MIN_SCALE) / 2f).coerceAtLeast(0f)

    /**
     * Folds a pinch ([zoomChange], multiplicative) and a drag ([panX]/[panY], pixels)
     * into [current]. Scale is clamped first; pan is then clamped to the edge bounds at
     * the new scale. Zooming back to the baseline snaps the offset to centre so the
     * image re-fits the viewport cleanly.
     */
    fun apply(
        current: ImageTransform,
        zoomChange: Float,
        panX: Float,
        panY: Float,
        viewportWidth: Float,
        viewportHeight: Float,
    ): ImageTransform {
        val newScale = clampScale(current.scale * zoomChange)
        if (newScale <= ImageTransform.MIN_SCALE + ImageTransform.EPSILON) {
            return ImageTransform()
        }
        val maxX = maxPan(viewportWidth, newScale)
        val maxY = maxPan(viewportHeight, newScale)
        return ImageTransform(
            scale = newScale,
            offsetX = (current.offsetX + panX).coerceIn(-maxX, maxX),
            offsetY = (current.offsetY + panY).coerceIn(-maxY, maxY),
        )
    }

    /**
     * Double-tap toggle: collapse back to the fitted baseline when already zoomed,
     * otherwise jump to [ImageTransform.DOUBLE_TAP_SCALE] centred on the viewport.
     */
    fun toggleDoubleTap(current: ImageTransform): ImageTransform =
        if (current.isZoomed) {
            ImageTransform()
        } else {
            ImageTransform(scale = ImageTransform.DOUBLE_TAP_SCALE)
        }
}
