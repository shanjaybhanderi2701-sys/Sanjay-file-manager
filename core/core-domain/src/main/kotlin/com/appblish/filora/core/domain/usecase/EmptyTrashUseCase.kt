package com.appblish.filora.core.domain.usecase

import com.appblish.filora.core.common.result.Result
import com.appblish.filora.core.domain.repository.TrashRepository
import javax.inject.Inject

/**
 * Empties the recycle bin — permanently deletes everything in it (T129, FR-3.4). The
 * destructive confirmation is the UI's responsibility. Returns the number of items
 * removed (0 when the bin was already empty).
 */
class EmptyTrashUseCase
    @Inject
    constructor(
        private val trashRepository: TrashRepository,
    ) {
        suspend operator fun invoke(): Result<Int> = trashRepository.emptyTrash()
    }
