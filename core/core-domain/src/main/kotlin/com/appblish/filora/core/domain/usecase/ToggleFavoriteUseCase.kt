package com.appblish.filora.core.domain.usecase

import com.appblish.filora.core.domain.model.FileItem
import com.appblish.filora.core.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Pins or unpins [item] as a favorite (FR-9.1) and persists the change. Reads the
 * current set first so a single user action toggles: if [item]'s path is already
 * pinned it is removed, otherwise it is added. Returns the resulting state (`true`
 * when now favorited) so the caller can update its star affordance without a round
 * trip through the observed stream.
 */
class ToggleFavoriteUseCase
    @Inject
    constructor(
        private val repository: FavoritesRepository,
    ) {
        suspend operator fun invoke(item: FileItem): Boolean {
            val alreadyFavorite = repository.observeFavorites().first().any { it.path == item.path }
            if (alreadyFavorite) {
                repository.removeFavorite(item.path)
            } else {
                repository.addFavorite(item)
            }
            return !alreadyFavorite
        }
    }
