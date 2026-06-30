package com.appblish.filora.core.domain.repository

import com.appblish.filora.core.common.result.Result
import com.appblish.filora.core.domain.model.FileItem
import com.appblish.filora.core.domain.model.StorageVolume
import kotlinx.coroutines.flow.Flow

/** Storage volumes and per-volume insights. */
interface StorageRepository {
    fun observeVolumes(): Flow<List<StorageVolume>>

    suspend fun getVolume(id: String): Result<StorageVolume>

    /** Largest files under [rootPath], descending by size, capped at [limit]. */
    suspend fun largestFiles(
        rootPath: String,
        limit: Int = 50
    ): Result<List<FileItem>>
}
