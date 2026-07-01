package com.appblish.filora.feature.media.preview

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Pure zoom/pan clamping rules behind the image preview (FR-6.1, FR-6.2). */
class ImageZoomMathTest {
    private val viewport = 1000f

    @Test
    fun `scale is clamped to the allowed range`() {
        assertThat(ImageZoomMath.clampScale(0.2f)).isEqualTo(ImageTransform.MIN_SCALE)
        assertThat(ImageZoomMath.clampScale(99f)).isEqualTo(ImageTransform.MAX_SCALE)
        assertThat(ImageZoomMath.clampScale(2.5f)).isEqualTo(2.5f)
    }

    @Test
    fun `a fitted image cannot pan`() {
        assertThat(ImageZoomMath.maxPan(viewport, ImageTransform.MIN_SCALE)).isEqualTo(0f)
    }

    @Test
    fun `pan range grows with scale as half the overflow`() {
        // At 2x, a 1000px-wide image overflows by 1000px → 500px each side.
        assertThat(ImageZoomMath.maxPan(viewport, 2f)).isEqualTo(500f)
    }

    @Test
    fun `pinch zooms in multiplicatively and clamps at max`() {
        val zoomed = ImageZoomMath.apply(ImageTransform(), zoomChange = 2f, panX = 0f, panY = 0f, viewport, viewport)
        assertThat(zoomed.scale).isEqualTo(2f)

        val pinnedToMax =
            ImageZoomMath.apply(ImageTransform(scale = 4f), zoomChange = 4f, panX = 0f, panY = 0f, viewport, viewport)
        assertThat(pinnedToMax.scale).isEqualTo(ImageTransform.MAX_SCALE)
    }

    @Test
    fun `pan is clamped to the edge bounds at the current scale`() {
        // At 2x the bound is ±500; a 9999px drag is clamped to the edge.
        val dragged =
            ImageZoomMath.apply(
                ImageTransform(scale = 2f),
                zoomChange = 1f,
                panX = 9999f,
                panY = -9999f,
                viewport,
                viewport
            )
        assertThat(dragged.offsetX).isEqualTo(500f)
        assertThat(dragged.offsetY).isEqualTo(-500f)
    }

    @Test
    fun `zooming back to baseline snaps the offset to centre`() {
        val panned = ImageTransform(scale = 3f, offsetX = 400f, offsetY = -200f)
        val reset = ImageZoomMath.apply(panned, zoomChange = 0.1f, panX = 0f, panY = 0f, viewport, viewport)
        assertThat(reset).isEqualTo(ImageTransform())
        assertThat(reset.isZoomed).isFalse()
    }

    @Test
    fun `double-tap toggles between baseline and the zoomed scale`() {
        val zoomedIn = ImageZoomMath.toggleDoubleTap(ImageTransform())
        assertThat(zoomedIn.scale).isEqualTo(ImageTransform.DOUBLE_TAP_SCALE)
        assertThat(zoomedIn.isZoomed).isTrue()

        val collapsed = ImageZoomMath.toggleDoubleTap(zoomedIn)
        assertThat(collapsed).isEqualTo(ImageTransform())
    }
}
