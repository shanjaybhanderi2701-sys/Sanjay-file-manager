package com.appblish.filora.core.data.thumbnail

/**
 * Thread-safe, access-ordered LRU bounded by a byte budget rather than an entry
 * count, so memory — not item quantity — drives eviction (NFR-9.2 bounded cache,
 * R6 memory-pressure mitigation).
 *
 * [sizeOf] reports each value's footprint in bytes (e.g. a bitmap's allocated
 * size). Inserting past [maxSizeBytes] evicts least-recently-used entries until
 * the total fits. A single entry larger than the whole budget is still stored,
 * then left as the sole survivor — never returning a value the caller just asked
 * to cache.
 *
 * The map is held as a singleton in the Hilt graph, so its contents outlive any
 * one screen and the cache survives navigation (FR-6.2 "cache survives navigation").
 * All access is guarded by an internal monitor; the cache is safe to share across
 * the coroutines decoding thumbnails concurrently.
 */
class BoundedLruCache<K : Any, V : Any>(
    private val maxSizeBytes: Long,
    private val sizeOf: (V) -> Int,
) {
    init {
        require(maxSizeBytes > 0) { "maxSizeBytes must be positive, was $maxSizeBytes" }
    }

    private val lock = Any()

    // accessOrder = true makes get()/put() move the touched key to the tail, so the
    // iterator's head is always the least-recently-used entry.
    private val entries = LinkedHashMap<K, V>(INITIAL_CAPACITY, LOAD_FACTOR, true)
    private var currentBytes = 0L

    /** Current total footprint of cached values, in bytes. */
    val sizeBytes: Long get() = synchronized(lock) { currentBytes }

    /** Number of entries currently held. */
    val count: Int get() = synchronized(lock) { entries.size }

    /** Returns the cached value for [key], marking it most-recently-used, or null. */
    fun get(key: K): V? = synchronized(lock) { entries[key] }

    /**
     * Caches [value] under [key], evicting LRU entries to stay within budget.
     * Returns the previous value for [key], if any.
     */
    fun put(
        key: K,
        value: V,
    ): V? =
        synchronized(lock) {
            val previous = entries.put(key, value)
            currentBytes += entrySize(value)
            if (previous != null) currentBytes -= entrySize(previous)
            trimToBudget()
            previous
        }

    /** Drops every entry and resets the byte total (e.g. on memory pressure). */
    fun clear(): Unit =
        synchronized(lock) {
            entries.clear()
            currentBytes = 0L
        }

    private fun entrySize(value: V): Long = sizeOf(value).coerceAtLeast(0).toLong()

    // The just-inserted key sits at the tail (most-recently-used), so evicting from
    // the head never removes it while more than one entry remains.
    private fun trimToBudget() {
        val iterator = entries.entries.iterator()
        while (currentBytes > maxSizeBytes && entries.size > 1 && iterator.hasNext()) {
            val eldest = iterator.next()
            currentBytes -= entrySize(eldest.value)
            iterator.remove()
        }
    }

    private companion object {
        const val INITIAL_CAPACITY = 16
        const val LOAD_FACTOR = 0.75f
    }
}
