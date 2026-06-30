package com.appblish.filora.core.domain.usecase

import com.appblish.filora.core.domain.model.FileItem
import com.appblish.filora.core.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Streams the most recently opened files/folders (FR-9.2), newest first, capped at
 * [DEFAULT_LIMIT]. Recents are deduplicated by path in the data layer, so a file
 * opened twice appears once with its latest timestamp. The cold [Flow] re-emits as
 * new opens are recorded.
 */
class ObserveRecentsUseCase
    @Inject
    constructor(
        private val repository: FavoritesRepository,
    ) {
        operator fun invoke(limit: Int = DEFAULT_LIMIT): Flow<List<FileItem>> = repository.observeRecents(limit)

        companion object {
            /** Default recents shown on the Home dashboard. */
            const val DEFAULT_LIMIT = 20
        }
    }
