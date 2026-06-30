package com.appblish.filora.core.domain.usecase

import com.appblish.filora.core.common.result.OperationError
import com.appblish.filora.core.common.result.Result
import com.appblish.filora.core.common.result.asError
import com.appblish.filora.core.common.result.asSuccess
import com.appblish.filora.core.domain.model.DeleteOutcome
import com.appblish.filora.core.domain.model.FileItem
import com.appblish.filora.core.domain.model.SortOrder
import com.appblish.filora.core.domain.repository.FileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * In-memory [FileRepository] for use-case tests. Records the arguments of the
 * last create/rename call and replays a scripted [Result] so tests can assert
 * both the delegation and the short-circuit (validation) paths.
 */
class FakeFileRepository(
    private val createResult: Result<FileItem> = sampleItem("New folder").asSuccess(),
    private val renameResult: Result<FileItem> = sampleItem("Renamed").asSuccess(),
    private val getFileResult: Result<FileItem> = sampleItem("Current").asSuccess(),
    private val deleteResult: Result<DeleteOutcome> = DeleteOutcome(deletedCount = 1, movedToTrash = true).asSuccess(),
    private val listing: Result<List<FileItem>> = emptyList<FileItem>().asSuccess(),
    private val copyFailurePaths: Set<String> = emptySet(),
    private val getFileByPath: ((String) -> Result<FileItem>)? = null,
) : FileRepository {
    var createFolderArgs: Pair<String, String>? = null
        private set
    var renameArgs: Pair<String, String>? = null
        private set
    var getFileArg: String? = null
        private set
    var deleteArgs: Pair<List<String>, Boolean>? = null
        private set

    /** Every [copy] call in order, for asserting resolved names and overwrite flags. */
    val copyCalls = mutableListOf<CopyCall>()

    override fun listDirectory(
        path: String,
        sortOrder: SortOrder,
    ): Flow<Result<List<FileItem>>> = flowOf(listing)

    override suspend fun getFile(path: String): Result<FileItem> {
        getFileArg = path
        return getFileByPath?.invoke(path) ?: getFileResult
    }

    override suspend fun createFolder(
        parentPath: String,
        name: String,
    ): Result<FileItem> {
        createFolderArgs = parentPath to name
        return createResult
    }

    override suspend fun rename(
        path: String,
        newName: String,
    ): Result<FileItem> {
        renameArgs = path to newName
        return renameResult
    }

    override suspend fun delete(
        paths: List<String>,
        toTrash: Boolean,
    ): Result<DeleteOutcome> {
        deleteArgs = paths to toTrash
        return deleteResult
    }

    override suspend fun copy(
        sourcePath: String,
        destinationDir: String,
        destinationName: String,
        overwrite: Boolean,
    ): Result<FileItem> {
        copyCalls += CopyCall(sourcePath, destinationDir, destinationName, overwrite)
        return if (sourcePath in copyFailurePaths) {
            OperationError.Io().asError()
        } else {
            FileItem(
                name = destinationName,
                path = "$destinationDir/$destinationName",
                isDirectory = false,
                sizeBytes = 0L,
                lastModifiedEpochMillis = 0L,
            ).asSuccess()
        }
    }

    /** A single recorded [copy] invocation. */
    data class CopyCall(
        val sourcePath: String,
        val destinationDir: String,
        val destinationName: String,
        val overwrite: Boolean,
    )

    companion object {
        fun sampleItem(
            name: String,
            isDirectory: Boolean = true,
            sizeBytes: Long = 0L,
        ): FileItem =
            FileItem(
                name = name,
                path = "/storage/emulated/0/$name",
                isDirectory = isDirectory,
                sizeBytes = sizeBytes,
                lastModifiedEpochMillis = 0L,
            )
    }
}
