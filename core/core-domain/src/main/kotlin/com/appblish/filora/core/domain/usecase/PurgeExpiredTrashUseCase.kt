package com.appblish.filora.core.domain.usecase

import com.appblish.filora.core.common.result.Result
import com.appblish.filora.core.domain.model.TrashRetention
import com.appblish.filora.core.domain.repository.TrashRepository
import javax.inject.Inject

/**
 * Applies the auto-purge policy (T128, FR-3.4): permanently deletes trashed items
 * older than [TrashRetention]'s window (default 30 days). Meant to run opportunistically
 * — on app start and when the Recycle Bin opens — so the bin can't grow unbounded.
 * Returns the number of items purged.
 */
class PurgeExpiredTrashUseCase
    @Inject
    constructor(
        private val trashRepository: TrashRepository,
    ) {
        suspend operator fun invoke(retention: TrashRetention = TrashRetention.Default): Result<Int> =
            trashRepository.purgeExpired(retention)
    }
