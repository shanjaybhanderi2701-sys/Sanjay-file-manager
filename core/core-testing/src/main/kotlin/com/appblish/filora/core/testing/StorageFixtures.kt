package com.appblish.filora.core.testing

import com.appblish.filora.core.common.result.OperationError
import com.appblish.filora.core.common.result.Result
import com.appblish.filora.core.common.result.asError
import com.appblish.filora.core.common.result.asSuccess
import com.appblish.filora.core.domain.model.FileItem
import com.appblish.filora.core.domain.model.StorageVolume
import com.appblish.filora.core.domain.repository.StorageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Ready-made [StorageVolume] fixtures: a primary internal volume plus a removable SD card. */
object StorageFixtures {
    val internalVolume: StorageVolume = StorageVolume(
        id = "primary",
        label = "Internal storage",
        rootPath = "/storage/emulated/0",
        totalBytes = 128_000_000_000,
        availableBytes = 48_000_000_000,
        isRemovable = false,
        isPrimary = true,
    )

    val sdCardVolume: StorageVolume = StorageVolume(
        id = "sdcard",
        label = "SD card",
        rootPath = "/storage/ABCD-1234",
        totalBytes = 64_000_000_000,
        availableBytes = 60_000_000_000,
        isRemovable = true,
        isPrimary = false,
    )

    val sampleVolumes: List<StorageVolume> = listOf(internalVolume, sdCardVolume)
}

/**
 * In-memory [StorageRepository] over a fixed volume list and a [largestFiles] result.
 * Set [error] to drive the failure branch for `getVolume`/`largestFiles`.
 */
class FakeStorageRepository(
    private val volumes: List<StorageVolume> = StorageFixtures.sampleVolumes,
    private val largest: List<FileItem> = emptyList(),
    private val error: OperationError? = null,
) : StorageRepository {
    override fun observeVolumes(): Flow<List<StorageVolume>> = flowOf(volumes)

    override suspend fun getVolume(id: String): Result<StorageVolume> =
        error?.asError() ?: volumes.firstOrNull { it.id == id }?.asSuccess() ?: OperationError.NotFound(id).asError()

    override suspend fun largestFiles(
        rootPath: String,
        limit: Int,
    ): Result<List<FileItem>> = error?.asError() ?: largest.sortedByDescending { it.sizeBytes }.take(limit).asSuccess()
}
