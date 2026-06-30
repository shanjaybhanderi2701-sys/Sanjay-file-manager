package com.appblish.filora.core.domain.repository

import com.appblish.filora.core.common.result.Result
import com.appblish.filora.core.domain.model.FileItem
import com.appblish.filora.core.domain.model.MediaCategory
import kotlinx.coroutines.flow.Flow

/** MediaStore-backed category browsing. */
interface MediaRepository {
    fun observeCategory(category: MediaCategory): Flow<Result<List<FileItem>>>

    /** Item counts per category for Home tiles. */
    suspend fun categoryCounts(): Result<Map<MediaCategory, Int>>
}
