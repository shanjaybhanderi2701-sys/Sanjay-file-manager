package com.appblish.filora.core.domain.usecase

import com.appblish.filora.core.common.result.OperationError
import com.appblish.filora.core.common.result.Result
import com.appblish.filora.core.common.result.asError
import com.appblish.filora.core.domain.repository.TrashRepository
import javax.inject.Inject

/**
 * Restores one or many items from the recycle bin to their original locations
 * (T126, FR-3.4). Blank ids are dropped and duplicates collapsed; an empty request is
 * rejected with [OperationError.NotFound] so a mis-wired caller fails loudly rather
 * than reporting a vacuous success. Returns the number of items restored.
 */
class RestoreFromTrashUseCase
    @Inject
    constructor(
        private val trashRepository: TrashRepository,
    ) {
        suspend operator fun invoke(ids: List<String>): Result<Int> {
            val targets = ids.filter { it.isNotBlank() }.distinct()
            return if (targets.isEmpty()) {
                OperationError.NotFound().asError()
            } else {
                trashRepository.restore(targets)
            }
        }
    }
