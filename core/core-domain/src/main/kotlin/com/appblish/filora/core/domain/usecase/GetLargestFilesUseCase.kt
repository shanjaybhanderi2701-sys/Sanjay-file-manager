package com.appblish.filora.core.domain.usecase

import com.appblish.filora.core.common.result.OperationError
import com.appblish.filora.core.common.result.Result
import com.appblish.filora.core.common.result.asError
import com.appblish.filora.core.common.result.map
import com.appblish.filora.core.common.result.runCatchingResult
import com.appblish.filora.core.domain.model.FileItem
import com.appblish.filora.core.domain.model.StorageVolume
import com.appblish.filora.core.domain.repository.StorageRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * The largest files on a storage volume (FR-8.2), descending by size and capped at
 * [DEFAULT_LIMIT]. Resolves the volume to scan from [StorageVolume.rootPath]:
 * the caller may name a [volumeId] (the storage breakdown lets the user drill into a
 * specific volume); otherwise the primary shared volume is scanned, falling back to
 * whatever volume is mounted.
 *
 * Zero-byte entries are dropped — they are noise in a "what's eating my space" view —
 * so the result is exactly the files worth offering delete/share on. A failure to
 * enumerate volumes surfaces as [OperationError.Io]; no mounted volume surfaces as
 * [OperationError.NotFound]; the scan's own errors propagate from
 * [StorageRepository.largestFiles] unchanged.
 */
class GetLargestFilesUseCase
    @Inject
    constructor(
        private val storageRepository: StorageRepository,
    ) {
        suspend operator fun invoke(
            volumeId: String? = null,
            limit: Int = DEFAULT_LIMIT,
        ): Result<List<FileItem>> {
            val volumesResult =
                runCatchingResult({ OperationError.Io(it) }) {
                    storageRepository.observeVolumes().first()
                }
            val volumes =
                when (volumesResult) {
                    is Result.Success -> volumesResult.data
                    is Result.Error -> return volumesResult
                }

            val target =
                volumes.firstOrNull { volumeId != null && it.id == volumeId }
                    ?: volumes.firstOrNull { it.isPrimary }
                    ?: volumes.firstOrNull()
                    ?: return OperationError.NotFound().asError()

            return storageRepository
                .largestFiles(target.rootPath, limit)
                .map { files -> files.filter { it.sizeBytes > 0L } }
        }

        companion object {
            /** Top-N default for the largest-files view (FR-8.2). */
            const val DEFAULT_LIMIT = 50
        }
    }
