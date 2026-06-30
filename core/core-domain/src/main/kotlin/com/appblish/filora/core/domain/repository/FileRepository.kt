package com.appblish.filora.core.domain.repository

import com.appblish.filora.core.common.result.Result
import com.appblish.filora.core.domain.model.DeleteOutcome
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

    /**
     * Deletes the items at [paths] (FR-3.4). When [toTrash] is true and the
     * location supports a recoverable trash (e.g. Android 11+ MediaStore), items
     * are moved there; otherwise they are removed permanently. The operation is
     * all-or-nothing: on any failure no items are deleted and an error is returned
     * (NFR-2.2). The returned [DeleteOutcome] reports whether trash was used so the
     * UI can offer Undo only when recovery is possible (NFR-2.4).
     */
    suspend fun delete(
        paths: List<String>,
        toTrash: Boolean = true
    ): Result<DeleteOutcome>

    /**
     * Copies the file or directory tree at [sourcePath] into [destinationDir],
     * naming the copy [destinationName]. An existing entry with that name is
     * replaced only when [overwrite] is true; otherwise the caller guarantees the
     * name is free (the conflict resolver has already picked one). Returns the
     * created entry so a move can verify the copy before deleting the source.
     *
     * There is deliberately no `move`: [com.appblish.filora.core.domain.usecase.MoveUseCase]
     * composes copy + verify + [delete] (FR-3.3 "move = copy-verify-delete") so the
     * repository contract stays minimal and the safety ordering lives in one place.
     */
    suspend fun copy(
        sourcePath: String,
        destinationDir: String,
        destinationName: String,
        overwrite: Boolean
    ): Result<FileItem>
}
