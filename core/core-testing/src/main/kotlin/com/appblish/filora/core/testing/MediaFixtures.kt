package com.appblish.filora.core.testing

import com.appblish.filora.core.common.result.OperationError
import com.appblish.filora.core.common.result.Result
import com.appblish.filora.core.common.result.asError
import com.appblish.filora.core.common.result.asSuccess
import com.appblish.filora.core.domain.model.FileItem
import com.appblish.filora.core.domain.model.MediaCategory
import com.appblish.filora.core.domain.repository.MediaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Ready-made [MediaRepository] fixtures. A default sample tree provides a handful of
 * items per category so Home-tile counts and category grids have realistic data
 * without each test hand-rolling MediaStore rows.
 */
object MediaFixtures {
    /** A small, deterministic set of items per category. */
    val sampleByCategory: Map<MediaCategory, List<FileItem>> = mapOf(
        MediaCategory.Images to listOf(
            mediaItem("beach.jpg", 1_200_000, "image/jpeg"),
            mediaItem("sunset.png", 800_000, "image/png"),
        ),
        MediaCategory.Video to listOf(
            mediaItem("clip.mp4", 24_000_000, "video/mp4"),
        ),
        MediaCategory.Audio to listOf(
            mediaItem("song.mp3", 4_500_000, "audio/mpeg"),
            mediaItem("podcast.m4a", 9_000_000, "audio/mp4"),
        ),
        MediaCategory.Documents to listOf(
            mediaItem("report.pdf", 250_000, "application/pdf"),
        ),
        MediaCategory.Archives to listOf(
            mediaItem("backup.zip", 12_000_000, "application/zip"),
        ),
    )

    /** Item counts derived from [sampleByCategory], zero for unlisted categories. */
    val sampleCounts: Map<MediaCategory, Int> =
        MediaCategory.entries.associateWith { (sampleByCategory[it] ?: emptyList()).size }

    /** Total bytes per category derived from [sampleByCategory]. */
    val sampleSizes: Map<MediaCategory, Long> =
        MediaCategory.entries.associateWith { category ->
            (sampleByCategory[category] ?: emptyList()).sumOf { it.sizeBytes }
        }

    private fun mediaItem(
        name: String,
        sizeBytes: Long,
        mimeType: String,
    ): FileItem =
        FileItem(
            name = name,
            path = "/storage/emulated/0/Media/$name",
            isDirectory = false,
            sizeBytes = sizeBytes,
            lastModifiedEpochMillis = 0L,
            mimeType = mimeType,
            extension = name.substringAfterLast('.', ""),
        )
}

/**
 * In-memory [MediaRepository] over a fixed category map (defaults to
 * [MediaFixtures.sampleByCategory]). Set [error] to drive the failure branch that
 * permission-aware screens render.
 */
class FakeMediaRepository(
    private val byCategory: Map<MediaCategory, List<FileItem>> = MediaFixtures.sampleByCategory,
    private val error: OperationError? = null,
) : MediaRepository {
    override fun observeCategory(category: MediaCategory): Flow<Result<List<FileItem>>> =
        flowOf(error?.asError() ?: (byCategory[category] ?: emptyList()).asSuccess())

    override suspend fun categoryCounts(): Result<Map<MediaCategory, Int>> =
        error?.asError() ?: MediaCategory.entries.associateWith { (byCategory[it] ?: emptyList()).size }.asSuccess()

    override suspend fun categorySizes(): Result<Map<MediaCategory, Long>> =
        error?.asError()
            ?: MediaCategory.entries
                .associateWith { c -> (byCategory[c] ?: emptyList()).sumOf { it.sizeBytes } }
                .asSuccess()
}
