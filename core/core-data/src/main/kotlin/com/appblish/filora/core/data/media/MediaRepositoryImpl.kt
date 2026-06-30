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
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
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
 *
 * [observeCategory] is a live view: it reads once on collection and then re-queries
 * each time [MediaStoreSource.changes] signals the collection changed (FR-6.1).
 * A revoked media permission surfaces as [OperationError.PermissionDenied] rather
 * than a generic [OperationError.Io], so the UI can route the user back to the
 * permission flow instead of showing a plain error.
 */
class MediaRepositoryImpl
    @Inject
    constructor(
        private val source: MediaStoreSource,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : MediaRepository {
        override fun observeCategory(category: MediaCategory): Flow<Result<List<FileItem>>> =
            source
                .changes()
                .onStart { emit(Unit) }
                .map {
                    runCatchingResult(::toOperationError) {
                        source.entriesIn(category).map { it.toFileItem() }
                    }
                }.flowOn(ioDispatcher)

        override suspend fun categoryCounts(): Result<Map<MediaCategory, Int>> =
            withContext(ioDispatcher) {
                runCatchingResult(::toOperationError) {
                    val counts = source.countByCategory()
                    MediaCategory.entries.associateWith { counts[it] ?: 0 }
                }
            }

        override suspend fun categorySizes(): Result<Map<MediaCategory, Long>> =
            withContext(ioDispatcher) {
                runCatchingResult(::toOperationError) {
                    val sizes = source.sizeByCategory()
                    MediaCategory.entries.associateWith { sizes[it] ?: 0L }
                }
            }

        /**
         * A revoked read-media permission throws [SecurityException] from the
         * resolver; map it to [OperationError.PermissionDenied] so callers can
         * re-prompt. Everything else is opaque platform I/O.
         */
        private fun toOperationError(t: Throwable): OperationError =
            when (t) {
                is SecurityException -> OperationError.PermissionDenied(t)
                else -> OperationError.Io(t)
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
