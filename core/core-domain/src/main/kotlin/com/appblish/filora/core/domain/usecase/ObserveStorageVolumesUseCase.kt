package com.appblish.filora.core.domain.usecase

import com.appblish.filora.core.domain.model.StorageVolume
import com.appblish.filora.core.domain.repository.StorageRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Streams the mounted storage volumes (FR-12.1) with their used/free totals, from
 * [StorageRepository.observeVolumes]. The cold [Flow] re-emits whenever a volume is
 * mounted or unmounted, so the Home dashboard's storage summary reflects live storage
 * state on resume without a manual refresh.
 *
 * This is the lightweight Home view of storage — just per-volume capacity, no
 * by-category slices. The full breakdown ([GetStorageBreakdownUseCase]) backs the
 * dedicated storage screen the summary hands off to.
 */
class ObserveStorageVolumesUseCase
    @Inject
    constructor(
        private val repository: StorageRepository,
    ) {
        operator fun invoke(): Flow<List<StorageVolume>> = repository.observeVolumes()
    }
