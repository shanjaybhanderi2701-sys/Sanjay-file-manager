package com.appblish.filora.core.domain.usecase

import com.appblish.filora.core.domain.model.FileItem
import com.appblish.filora.core.domain.repository.FavoritesRepository
import javax.inject.Inject

/**
 * Records that [item] was just opened (FR-9.2). The data layer upserts by path, so
 * re-opening an entry refreshes its timestamp rather than duplicating it. Called
 * from the open/play handoff; failures to persist are non-fatal to opening the file
 * and are swallowed by the repository.
 */
class RecordRecentUseCase
    @Inject
    constructor(
        private val repository: FavoritesRepository,
    ) {
        suspend operator fun invoke(item: FileItem) {
            repository.recordRecent(item)
        }
    }
