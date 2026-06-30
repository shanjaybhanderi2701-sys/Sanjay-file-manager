package com.appblish.filora.core.domain.usecase

import com.appblish.filora.core.domain.model.FileItem
import com.appblish.filora.core.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Streams the user's pinned favorites (FR-9.1), newest-pinned first, from the
 * Room-backed [FavoritesRepository]. The cold [Flow] re-emits whenever the set
 * changes, so the Home dashboard reflects a pin/unpin without a manual refresh.
 */
class ObserveFavoritesUseCase
    @Inject
    constructor(
        private val repository: FavoritesRepository,
    ) {
        operator fun invoke(): Flow<List<FileItem>> = repository.observeFavorites()
    }
