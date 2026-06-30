package com.appblish.filora.core.data.operations

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory side-channel for the (potentially large) source-path lists that a
 * file-operation work request carries. WorkManager's input [androidx.work.Data]
 * is capped at ~10 KB, which a multi-select of long SAF document URIs blows past
 * easily, so the scheduler stashes the list here keyed by the operation id and
 * passes only that id through `Data` (spec §2, FR-3.5).
 *
 * The store is process-scoped: it is sufficient for an operation enqueued and
 * run while the app process is alive, including across app backgrounding. It is
 * intentionally *not* the persistence mechanism for surviving process death —
 * that guarantee comes from WorkManager itself, and a worker that wakes to a
 * missing key (cold restart after the process was killed) treats it as a no-op
 * recovery rather than crashing (NFR-2.3).
 */
@Singleton
class WorkRequestStore
    @Inject
    constructor() {
        private val lists = ConcurrentHashMap<String, List<String>>()

        /** Stash [paths] under [key], replacing any previous value. */
        fun put(
            key: String,
            paths: List<String>
        ) {
            lists[key] = paths.toList()
        }

        /** Returns the stashed list for [key], or `null` if it was never stored or already consumed. */
        fun get(key: String): List<String>? = lists[key]

        /** Drops the list for [key]; call once the work for it has terminated. */
        fun remove(key: String) {
            lists.remove(key)
        }

        /** Visible for tests / diagnostics: how many lists are currently held. */
        val size: Int get() = lists.size
    }
