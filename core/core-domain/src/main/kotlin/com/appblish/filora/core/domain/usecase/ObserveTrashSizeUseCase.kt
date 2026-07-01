package com.appblish.filora.core.domain.usecase

import com.appblish.filora.core.domain.repository.TrashRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Streams the recycle bin's total footprint in bytes (T129, FR-3.4). */
class ObserveTrashSizeUseCase
    @Inject
    constructor(
        private val trashRepository: TrashRepository,
    ) {
        operator fun invoke(): Flow<Long> = trashRepository.observeTrashSize()
    }
