package com.appblish.filora.core.data.storage

import com.appblish.filora.core.common.dispatcher.IoDispatcher
import com.appblish.filora.core.common.result.OperationError
import com.appblish.filora.core.common.result.Result
import com.appblish.filora.core.common.result.asError
import com.appblish.filora.core.common.result.asSuccess
import com.appblish.filora.core.common.result.runCatchingResult
import com.appblish.filora.core.common.util.FileExtensions
import com.appblish.filora.core.domain.model.FileItem
import com.appblish.filora.core.domain.model.StorageVolume
import com.appblish.filora.core.domain.repository.StorageRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/**
 * [StorageRepository] backed by [VolumeEnumerator] for the platform listing and
 * `java.io.File` for byte sizing. Total/available bytes come from
 * [File.getTotalSpace]/[File.getUsableSpace], which work uniformly on any mounted
 * filesystem, so the FR-1.2 used/total reporting is computed in pure JVM code.
 */
class StorageRepositoryImpl
    @Inject
    constructor(
        private val enumerator: VolumeEnumerator,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : StorageRepository {
        override fun observeVolumes(): Flow<List<StorageVolume>> = flow { emit(loadVolumes()) }.flowOn(ioDispatcher)

        override suspend fun getVolume(id: String): Result<StorageVolume> =
            withContext(ioDispatcher) {
                runCatchingResult({ OperationError.Io(it) }) {
                    loadVolumes().firstOrNull { it.id == id }
                }.let { result ->
                    when (result) {
                        is Result.Success ->
                            result.data?.asSuccess()
                                ?: OperationError.NotFound(path = id).asError()
                        is Result.Error -> result
                    }
                }
            }

        override suspend fun largestFiles(
            rootPath: String,
            limit: Int,
        ): Result<List<FileItem>> =
            withContext(ioDispatcher) {
                val root = File(rootPath)
                when {
                    !root.exists() -> OperationError.NotFound(path = rootPath).asError()
                    limit <= 0 -> emptyList<FileItem>().asSuccess()
                    else ->
                        runCatchingResult({ OperationError.Io(it) }) {
                            root
                                .walkTopDown()
                                .onFail { _, _ -> }
                                .filter { it.isFile }
                                .toList()
                                .sortedByDescending { it.length() }
                                .take(limit)
                                .map { it.toFileItem() }
                        }
                }
            }

        private fun loadVolumes(): List<StorageVolume> = enumerator.enumerate().map { it.toStorageVolume() }
    }

/** Maps a platform volume to the domain model, sizing it from its mount directory. */
internal fun RawVolume.toStorageVolume(): StorageVolume {
    val dir = File(rootPath)
    return StorageVolume(
        id = id,
        label = label,
        rootPath = rootPath,
        totalBytes = dir.totalSpace,
        availableBytes = dir.usableSpace,
        isRemovable = isRemovable,
        isPrimary = isPrimary,
    )
}

internal fun File.toFileItem(): FileItem =
    FileItem(
        name = name,
        path = absolutePath,
        isDirectory = isDirectory,
        sizeBytes = length(),
        lastModifiedEpochMillis = lastModified(),
        extension = FileExtensions.extensionOf(name),
        isHidden = isHidden,
    )
