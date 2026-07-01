package com.appblish.filora.core.domain.usecase

import com.appblish.filora.core.common.result.OperationError
import com.appblish.filora.core.common.result.Result
import com.appblish.filora.core.common.result.asError
import com.appblish.filora.core.domain.repository.TrashRepository
import javax.inject.Inject

/**
 * Permanently deletes items from the recycle bin (T127, FR-3.4). This is irreversible;
 * the destructive confirmation is the UI's responsibility. Blank ids are dropped and
 * duplicates collapsed; an empty request is rejected with [OperationError.NotFound].
 * Returns the number of items permanently removed.
 */
class DeleteForeverUseCase
    @Inject
    constructor(
        private val trashRepository: TrashRepository,
    ) {
        suspend operator fun invoke(ids: List<String>): Result<Int> {
            val targets = ids.filter { it.isNotBlank() }.distinct()
            return if (targets.isEmpty()) {
                OperationError.NotFound().asError()
            } else {
                trashRepository.deleteForever(targets)
            }
        }
    }
