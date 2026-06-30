package com.appblish.filora.core.domain.repository

import com.appblish.filora.core.common.result.Result
import com.appblish.filora.core.domain.model.FileItem

/** Zip compression/extraction. Path-traversal safety is enforced in `core-data`. */
interface ArchiveRepository {
    suspend fun extract(
        archivePath: String,
        destinationDir: String
    ): Result<Unit>

    suspend fun compress(
        sources: List<String>,
        destinationArchivePath: String
    ): Result<FileItem>

    suspend fun listEntries(archivePath: String): Result<List<String>>
}
