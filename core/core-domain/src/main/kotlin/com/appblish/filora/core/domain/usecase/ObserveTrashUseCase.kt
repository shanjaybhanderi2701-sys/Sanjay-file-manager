package com.appblish.filora.core.domain.usecase

import com.appblish.filora.core.domain.model.TrashedItem
import com.appblish.filora.core.domain.repository.TrashRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Streams the recycle-bin contents for the Recycle Bin screen (T125, FR-3.4). */
class ObserveTrashUseCase
    @Inject
    constructor(
        private val trashRepository: TrashRepository,
    ) {
        operator fun invoke(): Flow<List<TrashedItem>> = trashRepository.observeTrash()
    }
