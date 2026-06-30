package com.appblish.filora.core.data.thumbnail

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ThumbnailSamplingTest {
    @Test
    fun `source already at or below request needs no downsample`() {
        assertThat(ThumbnailSampling.calculateInSampleSize(200, 200, 200, 200)).isEqualTo(1)
        assertThat(ThumbnailSampling.calculateInSampleSize(100, 100, 200, 200)).isEqualTo(1)
    }

    @Test
    fun `sample size is the largest power of two that stays at or above the request`() {
        // 4000x3000 down to a 200x200 cell -> /8 keeps 500x375 (still >= 200), /16 would undershoot.
        assertThat(ThumbnailSampling.calculateInSampleSize(4000, 3000, 200, 200)).isEqualTo(8)
        // 1000x1000 -> /4 keeps 250 (>= 200), /8 would drop to 125.
        assertThat(ThumbnailSampling.calculateInSampleSize(1000, 1000, 200, 200)).isEqualTo(4)
        // 500x500 -> /2 keeps 250 (>= 200), /4 would drop to 125.
        assertThat(ThumbnailSampling.calculateInSampleSize(500, 500, 200, 200)).isEqualTo(2)
    }

    @Test
    fun `non-positive dimensions fall back to no downsample`() {
        assertThat(ThumbnailSampling.calculateInSampleSize(0, 100, 50, 50)).isEqualTo(1)
        assertThat(ThumbnailSampling.calculateInSampleSize(100, 100, 0, 50)).isEqualTo(1)
        assertThat(ThumbnailSampling.calculateInSampleSize(-1, -1, -1, -1)).isEqualTo(1)
    }
}
