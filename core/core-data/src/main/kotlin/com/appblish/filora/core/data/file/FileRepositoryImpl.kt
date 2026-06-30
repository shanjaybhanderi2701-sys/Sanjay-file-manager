package com.appblish.filora.core.data.file

import com.appblish.filora.core.common.dispatcher.IoDispatcher
import com.appblish.filora.core.common.result.OperationError
import com.appblish.filora.core.common.result.Result
import com.appblish.filora.core.common.result.runCatchingResult
import com.appblish.filora.core.domain.model.DeleteOutcome
import com.appblish.filora.core.domain.model.FileItem
import com.appblish.filora.core.domain.model.SortOrder
import com.appblish.filora.core.domain.model.ordered
import com.appblish.filora.core.domain.repository.FileRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import java.io.IOException
import javax.inject.Inject

/**
 * The single [FileRepository] the app depends on (T034, FR-2.1). It picks the backend by
 * the location's scheme — a `content://` tree-document URI is SAF ([SafDataSource]),
 * anything else is a local java.io path ([FileSystemDataSource]) — so use cases and the
 * Browser never branch on storage scope or API level.
 *
 * All work runs on [ioDispatcher]: [listDirectory] streams on a cold [flow]/[flowOn] and
 * the operations use [withContext], so blocking I/O never touches the caller's thread
 * (NFR-1). Raw exceptions are translated into the [OperationError] taxonomy here and never
 * cross the layer boundary.
 *
 * **Scope note:** SAF write operations are partial — `copy` of a directory is not yet
 * supported and surfaces a clean [OperationError.Io] rather than a half-written tree. The
 * local java.io path implements every operation fully.
 */
internal class FileRepositoryImpl
    @Inject
    constructor(
        private val fileSystem: FileSystemDataSource,
        private val saf: SafDataSource,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : FileRepository {
        override fun listDirectory(
            path: String,
            sortOrder: SortOrder,
        ): Flow<Result<List<FileItem>>> =
            flow {
                emit(
                    runCatchingResult(map = { it.toOperationError(path) }) {
                        sourceFor(path).list(path).ordered(sortOrder)
                    },
                )
            }.flowOn(ioDispatcher)

        override suspend fun getFile(path: String): Result<FileItem> = io(path) { sourceFor(path).getFile(path) }

        override suspend fun createFolder(
            parentPath: String,
            name: String,
        ): Result<FileItem> = io(parentPath) { sourceFor(parentPath).createFolder(parentPath, name) }

        override suspend fun rename(
            path: String,
            newName: String,
        ): Result<FileItem> = io(path) { sourceFor(path).rename(path, newName) }

        override suspend fun delete(
            paths: List<String>,
            toTrash: Boolean,
        ): Result<DeleteOutcome> =
            io(paths.firstOrNull()) {
                // Trash is a MediaStore affordance; java.io/SAF removal is permanent.
                paths.groupBy(::isSaf).forEach { (saf, group) ->
                    sourceFor(saf).delete(group)
                }
                DeleteOutcome(deletedCount = paths.size, movedToTrash = false)
            }

        override suspend fun copy(
            sourcePath: String,
            destinationDir: String,
            destinationName: String,
            overwrite: Boolean,
        ): Result<FileItem> =
            io(sourcePath) {
                sourceFor(destinationDir).copy(sourcePath, destinationDir, destinationName, overwrite)
            }

        private fun sourceFor(location: String): FileSource = sourceFor(isSaf(location))

        private fun sourceFor(saf: Boolean): FileSource = if (saf) this.saf else fileSystem

        private suspend fun <T> io(
            path: String?,
            block: () -> T,
        ): Result<T> =
            withContext(ioDispatcher) {
                runCatchingResult(map = { it.toOperationError(path) }, block = block)
            }

        private companion object {
            const val CONTENT_SCHEME = "content://"

            fun isSaf(location: String): Boolean = location.startsWith(CONTENT_SCHEME)
        }
    }

/** Translates platform exceptions into the domain error taxonomy (NFR-2). */
private fun Throwable.toOperationError(path: String?): OperationError =
    when (this) {
        is SecurityException -> OperationError.PermissionDenied(this)
        is FileNotFoundException -> OperationError.NotFound(path, this)
        is java.nio.file.FileAlreadyExistsException -> OperationError.Conflict(path, this)
        is IOException -> OperationError.Io(this)
        else -> OperationError.Unknown(this)
    }
