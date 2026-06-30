package com.appblish.filora.core.domain.repository

import com.appblish.filora.core.common.result.Result
import com.appblish.filora.core.domain.model.FileItem
import com.appblish.filora.core.domain.model.SortOrder
import kotlinx.coroutines.flow.Flow

/**
 * Directory listing and file operations. The implementation in `core-data` picks
 * the correct source (java.io / SAF / MediaStore) per location; callers never
 * branch on storage scope or API level.
 */
interface FileRepository {
    /** Streams the contents of [path], re-emitting on underlying changes. */
    fun listDirectory(
        path: String,
        sortOrder: SortOrder = SortOrder.Default
    ): Flow<Result<List<FileItem>>>

    suspend fun getFile(path: String): Result<FileItem>

    suspend fun createFolder(
        parentPath: String,
        name: String
    ): Result<FileItem>

    suspend fun rename(
        path: String,
        newName: String
    ): Result<FileItem>

    suspend fun delete(paths: List<String>): Result<Unit>

    suspend fun copy(
        sources: List<String>,
        destinationDir: String
    ): Result<Unit>

    suspend fun move(
        sources: List<String>,
        destinationDir: String
    ): Result<Unit>
}
