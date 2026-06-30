package com.appblish.filora.core.domain.usecase

import com.appblish.filora.core.common.result.Result
import com.appblish.filora.core.domain.model.FileItem
import com.appblish.filora.core.domain.model.SearchQuery
import com.appblish.filora.core.domain.repository.FileRepository
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Searches the current tree by name substring (FR-5.1), emitting each match as
 * soon as it is found rather than collecting the whole tree first.
 *
 * The returned [Flow] is **cold and cancelable**: traversal only runs while a
 * collector is active, and because it cooperates with cancellation
 * ([ensureActive] before every directory listing) a UI that abandons the search —
 * the user keeps typing, navigates away — stops the walk promptly instead of
 * draining a deep tree. Callers debounce keystrokes upstream and collect with
 * `collectLatest`/`flatMapLatest` so a newer query cancels the previous walk.
 *
 * Traversal is breadth-first from [SearchQuery.rootPath]. A directory that cannot
 * be listed (permission denied, removed mid-walk) is skipped rather than aborting
 * the whole search, so results from every readable subtree still stream through
 * (NFR-2.2 partial progress). Matching is case-insensitive and applies to both
 * files and folders; hidden entries are visited and emitted only when
 * [SearchQuery.includeHidden] is set.
 *
 * Type/size/date filter combination is layered on top in T5.2; this use case is
 * the name-substring streaming primitive it builds on.
 */
class SearchFilesUseCase
    @Inject
    constructor(
        private val fileRepository: FileRepository,
    ) {
        operator fun invoke(query: SearchQuery): Flow<FileItem> =
            flow {
                val root = query.rootPath
                if (query.text.isBlank() || root == null) return@flow

                val needle = query.text.trim()
                val pending = ArrayDeque<String>().apply { add(root) }
                while (pending.isNotEmpty()) {
                    // Honor cancellation between listings even if the repository
                    // returns synchronously (e.g. a cached/fake source).
                    currentCoroutineContext().ensureActive()

                    val dir = pending.removeFirst()
                    val items =
                        when (val listing = fileRepository.listDirectory(dir, query.sortOrder).first()) {
                            is Result.Success -> listing.data
                            is Result.Error -> continue // unreadable subtree: skip, keep searching
                        }

                    for (item in items) {
                        if (item.isHidden && !query.includeHidden) continue
                        if (item.name.contains(needle, ignoreCase = true)) emit(item)
                        if (item.isDirectory) pending.add(item.path)
                    }
                }
            }
    }
