package com.appblish.filora.core.data.thumbnail

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Each value reports a fixed size so byte-budget eviction is deterministic. The
 * cache is keyed by String, valued by [Sized], sized at [Sized.bytes].
 */
class BoundedLruCacheTest {
    private data class Sized(
        val bytes: Int
    )

    private fun cache(maxBytes: Long) = BoundedLruCache<String, Sized>(maxBytes) { it.bytes }

    @Test
    fun `get returns stored value and tracks byte total`() {
        val cache = cache(maxBytes = 100)
        cache.put("a", Sized(30))

        assertThat(cache.get("a")).isEqualTo(Sized(30))
        assertThat(cache.sizeBytes).isEqualTo(30)
        assertThat(cache.count).isEqualTo(1)
    }

    @Test
    fun `missing key returns null`() {
        assertThat(cache(maxBytes = 100).get("nope")).isNull()
    }

    @Test
    fun `eviction drops least-recently-used entries to stay within budget`() {
        val cache = cache(maxBytes = 100)
        cache.put("a", Sized(40))
        cache.put("b", Sized(40))
        // Touch "a" so "b" becomes least-recently-used.
        cache.get("a")

        cache.put("c", Sized(40)) // total would be 120 > 100, so one entry is evicted

        assertThat(cache.get("b")).isNull()
        assertThat(cache.get("a")).isNotNull()
        assertThat(cache.get("c")).isNotNull()
        assertThat(cache.sizeBytes).isEqualTo(80)
    }

    @Test
    fun `re-putting same key replaces value without double-counting bytes`() {
        val cache = cache(maxBytes = 100)
        cache.put("a", Sized(30))
        cache.put("a", Sized(50))

        assertThat(cache.get("a")).isEqualTo(Sized(50))
        assertThat(cache.sizeBytes).isEqualTo(50)
        assertThat(cache.count).isEqualTo(1)
    }

    @Test
    fun `entry larger than budget is kept as the sole survivor`() {
        val cache = cache(maxBytes = 50)
        cache.put("small", Sized(10))
        cache.put("huge", Sized(200))

        assertThat(cache.get("huge")).isNotNull()
        assertThat(cache.get("small")).isNull()
        assertThat(cache.count).isEqualTo(1)
    }

    @Test
    fun `clear empties the cache and resets the byte total`() {
        val cache = cache(maxBytes = 100)
        cache.put("a", Sized(30))
        cache.put("b", Sized(20))

        cache.clear()

        assertThat(cache.count).isEqualTo(0)
        assertThat(cache.sizeBytes).isEqualTo(0)
        assertThat(cache.get("a")).isNull()
    }

    @Test
    fun `non-positive max size is rejected`() {
        runCatching { BoundedLruCache<String, Sized>(0) { it.bytes } }
            .let { assertThat(it.isFailure).isTrue() }
    }

    @Test
    fun `negative element size is clamped to zero so the total never goes negative`() {
        val cache = BoundedLruCache<String, Sized>(100) { it.bytes }
        cache.put("weird", Sized(-10))

        assertThat(cache.sizeBytes).isEqualTo(0)
        assertThat(cache.get("weird")).isNotNull()
    }
}
