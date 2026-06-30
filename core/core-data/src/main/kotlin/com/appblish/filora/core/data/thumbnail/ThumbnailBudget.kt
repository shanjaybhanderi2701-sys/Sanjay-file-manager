package com.appblish.filora.core.data.thumbnail

/**
 * Derives the thumbnail cache's byte budget from the process heap so it scales with
 * device RAM instead of a hard-coded constant (R6 "OOM on low-RAM devices"). The
 * concrete heap figure is read from the platform at provide-time; the arithmetic
 * lives here so it stays unit-testable.
 */
object ThumbnailBudget {
    /** Fraction of the max heap handed to the thumbnail cache by default. */
    const val DEFAULT_HEAP_FRACTION = 0.25

    /** Floor so the cache is never starved on a tiny or unreported heap. */
    const val MIN_BYTES = 2L * 1024 * 1024

    /**
     * [fraction] of [maxHeapBytes], clamped to at least [MIN_BYTES]. Falls back to
     * [MIN_BYTES] when the heap size is unknown (non-positive).
     */
    fun maxBytes(
        maxHeapBytes: Long,
        fraction: Double = DEFAULT_HEAP_FRACTION,
    ): Long {
        require(fraction > 0.0 && fraction <= 1.0) { "fraction must be in (0, 1], was $fraction" }
        if (maxHeapBytes <= 0L) return MIN_BYTES
        return (maxHeapBytes * fraction).toLong().coerceAtLeast(MIN_BYTES)
    }
}
