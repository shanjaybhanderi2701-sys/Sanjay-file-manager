package com.appblish.filora.core.domain.usecase

import com.appblish.filora.core.common.result.Result
import com.appblish.filora.core.domain.model.FileItem
import com.appblish.filora.core.domain.model.SortOrder
import com.appblish.filora.core.domain.repository.FileRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Streams the contents of a directory for the Browser (FR-2.1). The returned [Flow]
 * re-emits a fresh [Result] snapshot whenever the repository re-reads the location —
 * on first collection and on every pull-to-refresh (FR-2.5) — so the ViewModel maps
 * straight to loading/empty/error/content without branching on storage scope.
 *
 * Ordering (folders-first default, then the chosen [sortOrder]) is applied by the
 * repository through the shared [com.appblish.filora.core.domain.model.ordered]
 * comparator, so callers never sort the list themselves. Hidden (dot) entries are
 * included in the stream; the ViewModel filters them per the user's show-hidden
 * preference (FR-2.4) so toggling never needs a re-read.
 */
class ListDirectoryUseCase
    @Inject
    constructor(
        private val fileRepository: FileRepository,
    ) {
        operator fun invoke(
            path: String,
            sortOrder: SortOrder = SortOrder.Default,
        ): Flow<Result<List<FileItem>>> = fileRepository.listDirectory(path, sortOrder)
    }
