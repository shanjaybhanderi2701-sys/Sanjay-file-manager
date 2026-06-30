package com.appblish.filora.core.domain.usecase

import com.appblish.filora.core.common.result.OperationError
import com.appblish.filora.core.common.result.Result
import com.appblish.filora.core.common.result.asError
import com.appblish.filora.core.common.result.asSuccess
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
) : FileRepository {
    var createFolderArgs: Pair<String, String>? = null
        private set
    var renameArgs: Pair<String, String>? = null
        private set
    var getFileArg: String? = null
        private set

    override fun listDirectory(
        path: String,
        sortOrder: SortOrder,
    ): Flow<Result<List<FileItem>>> = flowOf(emptyList<FileItem>().asSuccess())

    override suspend fun getFile(path: String): Result<FileItem> {
        getFileArg = path
        return getFileResult
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

    override suspend fun delete(paths: List<String>): Result<Unit> = Unit.asSuccess()

    override suspend fun copy(
        sources: List<String>,
        destinationDir: String,
    ): Result<Unit> = OperationError.Unknown().asError()

    override suspend fun move(
        sources: List<String>,
        destinationDir: String,
    ): Result<Unit> = OperationError.Unknown().asError()

    companion object {
        fun sampleItem(name: String): FileItem =
            FileItem(
                name = name,
                path = "/storage/emulated/0/$name",
                isDirectory = true,
                sizeBytes = 0L,
                lastModifiedEpochMillis = 0L,
            )
    }
}
