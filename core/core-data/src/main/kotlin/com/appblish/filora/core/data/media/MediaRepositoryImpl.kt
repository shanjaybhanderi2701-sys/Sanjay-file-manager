package com.appblish.filora.core.data.media

import com.appblish.filora.core.common.dispatcher.IoDispatcher
import com.appblish.filora.core.common.result.OperationError
import com.appblish.filora.core.common.result.Result
import com.appblish.filora.core.common.result.runCatchingResult
import com.appblish.filora.core.common.util.FileExtensions
import com.appblish.filora.core.domain.model.FileItem
import com.appblish.filora.core.domain.model.MediaCategory
import com.appblish.filora.core.domain.repository.MediaRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * [MediaRepository] backed by [MediaStoreSource]. The platform cursor work and
 * per-row bucketing live behind the source ([AndroidMediaStoreSource] +
 * [MediaClassifier]); this layer only moves work onto the IO dispatcher and turns
 * thrown platform errors into [Result] values so they never cross into use cases.
 *
 * [categoryCounts] returns a dense map — every [MediaCategory] is present, with a
 * zero for categories that have no items — so Home tiles can render without
 * null-handling.
 */
class MediaRepositoryImpl
    @Inject
    constructor(
        private val source: MediaStoreSource,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : MediaRepository {
        override fun observeCategory(category: MediaCategory): Flow<Result<List<FileItem>>> =
            flow {
                emit(
                    runCatchingResult({ OperationError.Io(it) }) {
                        source.entriesIn(category).map { it.toFileItem() }
                    },
                )
            }.flowOn(ioDispatcher)

        override suspend fun categoryCounts(): Result<Map<MediaCategory, Int>> =
            withContext(ioDispatcher) {
                runCatchingResult({ OperationError.Io(it) }) {
                    val counts = source.countByCategory()
                    MediaCategory.entries.associateWith { counts[it] ?: 0 }
                }
            }
    }

/** Maps a raw MediaStore entry to the platform-agnostic domain model. */
internal fun RawMediaEntry.toFileItem(): FileItem =
    FileItem(
        name = displayName,
        path = contentUri,
        isDirectory = false,
        sizeBytes = sizeBytes,
        lastModifiedEpochMillis = dateModifiedEpochMillis,
        mimeType = mimeType,
        extension = FileExtensions.extensionOf(displayName),
    )
