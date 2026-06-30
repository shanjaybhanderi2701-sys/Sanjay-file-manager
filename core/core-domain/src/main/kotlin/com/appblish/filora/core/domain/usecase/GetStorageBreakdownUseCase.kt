package com.appblish.filora.core.domain.usecase

import com.appblish.filora.core.common.result.OperationError
import com.appblish.filora.core.common.result.Result
import com.appblish.filora.core.domain.model.CategoryUsage
import com.appblish.filora.core.domain.model.MediaCategory
import com.appblish.filora.core.domain.model.StorageBreakdown
import com.appblish.filora.core.domain.model.StorageVolume
import com.appblish.filora.core.domain.model.VolumeBreakdown
import com.appblish.filora.core.domain.repository.MediaRepository
import com.appblish.filora.core.domain.repository.StorageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Assembles the per-volume, by-category storage breakdown (FR-8.1) by combining the
 * volume list ([StorageRepository.observeVolumes]) with the media index's by-category
 * bytes/counts ([MediaRepository.categorySizes]/[MediaRepository.categoryCounts]).
 *
 * The result is a cold [Flow] that re-emits whenever the volume list changes. Each
 * emission pairs every volume's used/free with category slices; because MediaStore
 * indexes the **primary** shared volume, only the primary volume receives slices —
 * removable volumes report used/free with empty categories.
 *
 * The screen must still render used/free even when media access is denied, so a media
 * failure **degrades** to empty category slices rather than failing the whole stream;
 * only a failure to enumerate volumes surfaces as [Result.Error]. Categories with zero
 * bytes are dropped, and the kept slices are ordered largest-first.
 */
class GetStorageBreakdownUseCase
    @Inject
    constructor(
        private val storageRepository: StorageRepository,
        private val mediaRepository: MediaRepository,
    ) {
        operator fun invoke(): Flow<Result<StorageBreakdown>> =
            storageRepository
                .observeVolumes()
                .map { volumes -> Result.Success(buildBreakdown(volumes)) as Result<StorageBreakdown> }
                .catch { emit(Result.Error(OperationError.Io(it))) }

        private suspend fun buildBreakdown(volumes: List<StorageVolume>): StorageBreakdown {
            val slices = primaryVolumeSlices()
            return StorageBreakdown(
                volumes =
                    volumes.map { volume ->
                        VolumeBreakdown(
                            volume = volume,
                            categories = if (volume.isPrimary) slices else emptyList(),
                        )
                    },
            )
        }

        /**
         * Category slices for the media-indexed (primary) volume, largest-first with
         * zero-byte categories removed. A media-access failure degrades to no slices.
         */
        private suspend fun primaryVolumeSlices(): List<CategoryUsage> {
            val sizes = (mediaRepository.categorySizes() as? Result.Success)?.data ?: return emptyList()
            val counts = (mediaRepository.categoryCounts() as? Result.Success)?.data.orEmpty()
            return MediaCategory.entries
                .mapNotNull { category ->
                    val bytes = sizes[category] ?: 0L
                    if (bytes <= 0L) {
                        null
                    } else {
                        CategoryUsage(
                            category = category,
                            sizeBytes = bytes,
                            itemCount = counts[category] ?: 0,
                        )
                    }
                }.sortedByDescending { it.sizeBytes }
        }
    }
