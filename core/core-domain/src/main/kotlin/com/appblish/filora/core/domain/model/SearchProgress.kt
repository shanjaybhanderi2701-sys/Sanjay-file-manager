package com.appblish.filora.core.domain.model

/**
 * A single emission from a streaming search ([com.appblish.filora.core.domain.repository.SearchRepository]).
 *
 * The walk publishes a [Match] for every entry that passes the query as soon as it is
 * found — the collector renders results incrementally rather than waiting for the whole
 * tree — and a single terminal [Completed] when traversal finishes normally. A search
 * that is cancelled (newer query, screen left) simply stops emitting; no [Completed]
 * arrives, which is how a collector distinguishes "finished" from "abandoned".
 */
sealed interface SearchProgress {
    /** An entry that matched the query. [matchCount] is the running total including this one. */
    data class Match(
        val item: FileItem,
        val matchCount: Int
    ) : SearchProgress

    /** Terminal marker emitted once the readable tree is exhausted. */
    data class Completed(
        val matchCount: Int
    ) : SearchProgress
}
