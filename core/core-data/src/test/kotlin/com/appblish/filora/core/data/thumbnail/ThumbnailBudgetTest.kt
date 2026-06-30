package com.appblish.filora.core.data.thumbnail

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ThumbnailBudgetTest {
    @Test
    fun `budget is the configured fraction of a healthy heap`() {
        val heap = 256L * 1024 * 1024
        assertThat(ThumbnailBudget.maxBytes(heap, fraction = 0.25)).isEqualTo(64L * 1024 * 1024)
    }

    @Test
    fun `budget never drops below the floor on a small heap`() {
        val tinyHeap = 4L * 1024 * 1024
        assertThat(ThumbnailBudget.maxBytes(tinyHeap, fraction = 0.25)).isEqualTo(ThumbnailBudget.MIN_BYTES)
    }

    @Test
    fun `unknown heap falls back to the floor`() {
        assertThat(ThumbnailBudget.maxBytes(0)).isEqualTo(ThumbnailBudget.MIN_BYTES)
        assertThat(ThumbnailBudget.maxBytes(-1)).isEqualTo(ThumbnailBudget.MIN_BYTES)
    }

    @Test
    fun `out-of-range fraction is rejected`() {
        assertThat(runCatching { ThumbnailBudget.maxBytes(1024, fraction = 0.0) }.isFailure).isTrue()
        assertThat(runCatching { ThumbnailBudget.maxBytes(1024, fraction = 1.5) }.isFailure).isTrue()
    }
}
