package com.appblish.filora.core.domain.repository

import com.appblish.filora.core.domain.model.SearchProgress
import com.appblish.filora.core.domain.model.SearchQuery
import kotlinx.coroutines.flow.Flow

/**
 * Streaming tree search over a storage root (FR-5.1). Implementations walk a java.io
 * filesystem path or a SAF tree-document URI, emit each match as it is found, and run
 * the walk on the I/O dispatcher.
 *
 * The returned [Flow] is **cold and cooperatively cancelable**: traversal starts only
 * when collected, and a collector that stops (debounced keystroke supersedes the query,
 * the screen is left) cancels the walk promptly instead of draining a deep tree. A blank
 * query — no text and no active [SearchQuery.filter] — short-circuits to a single
 * [SearchProgress.Completed] without listing anything.
 */
interface SearchRepository {
    fun search(query: SearchQuery): Flow<SearchProgress>
}
