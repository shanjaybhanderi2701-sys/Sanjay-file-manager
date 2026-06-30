package com.appblish.filora.core.domain.usecase

import com.appblish.filora.core.common.result.OperationError
import com.appblish.filora.core.common.result.Result
import com.appblish.filora.core.domain.model.FileItem
import com.appblish.filora.core.domain.model.MediaCategory
import com.appblish.filora.core.domain.model.StorageVolume
import com.appblish.filora.core.domain.repository.MediaRepository
import com.appblish.filora.core.domain.repository.StorageRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException

class GetStorageBreakdownUseCaseTest {
    private fun volume(
        id: String,
        isPrimary: Boolean,
        isRemovable: Boolean = !isPrimary,
        total: Long = 1_000,
        available: Long = 400,
    ) = StorageVolume(
        id = id,
        label = id,
        rootPath = "/storage/$id",
        totalBytes = total,
        availableBytes = available,
        isRemovable = isRemovable,
        isPrimary = isPrimary,
    )

    private class FakeStorage(
        private val flow: Flow<List<StorageVolume>>,
    ) : StorageRepository {
        override fun observeVolumes(): Flow<List<StorageVolume>> = flow

        override suspend fun getVolume(id: String): Result<StorageVolume> = throw UnsupportedOperationException()

        override suspend fun largestFiles(
            rootPath: String,
            limit: Int,
        ): Result<List<FileItem>> = throw UnsupportedOperationException()
    }

    private class FakeMedia(
        private val sizes: Result<Map<MediaCategory, Long>>,
        private val counts: Result<Map<MediaCategory, Int>> = Result.Success(emptyMap()),
    ) : MediaRepository {
        override fun observeCategory(category: MediaCategory): Flow<Result<List<FileItem>>> =
            flowOf(Result.Success(emptyList()))

        override suspend fun categoryCounts(): Result<Map<MediaCategory, Int>> = counts

        override suspend fun categorySizes(): Result<Map<MediaCategory, Long>> = sizes
    }

    @Test
    fun `attaches category slices to the primary volume only, largest-first`() =
        runTest {
            val storage =
                FakeStorage(flowOf(listOf(volume("internal", isPrimary = true), volume("sdcard", isPrimary = false))))
            val media =
                FakeMedia(
                    sizes =
                        Result.Success(
                            mapOf(
                                MediaCategory.Images to 300L,
                                MediaCategory.Video to 900L,
                                MediaCategory.Audio to 0L,
                            ),
                        ),
                    counts = Result.Success(mapOf(MediaCategory.Images to 3, MediaCategory.Video to 9)),
                )

            val result = GetStorageBreakdownUseCase(storage, media)().first()

            assertThat(result).isInstanceOf(Result.Success::class.java)
            val breakdown = (result as Result.Success).data
            assertThat(breakdown.volumes).hasSize(2)

            val primary = breakdown.volumes.first { it.volume.isPrimary }
            // Video (900) before Images (300); the zero-byte Audio slice is dropped.
            assertThat(primary.categories.map { it.category })
                .containsExactly(MediaCategory.Video, MediaCategory.Images)
                .inOrder()
            assertThat(primary.categories.first().sizeBytes).isEqualTo(900L)
            assertThat(primary.categories.first().itemCount).isEqualTo(9)
            assertThat(primary.categorizedBytes).isEqualTo(1_200L)

            val removable = breakdown.volumes.first { !it.volume.isPrimary }
            assertThat(removable.categories).isEmpty()
        }

    @Test
    fun `degrades to volumes without slices when media access fails`() =
        runTest {
            val storage = FakeStorage(flowOf(listOf(volume("internal", isPrimary = true))))
            val media = FakeMedia(sizes = Result.Error(OperationError.PermissionDenied()))

            val result = GetStorageBreakdownUseCase(storage, media)().first()

            assertThat(result).isInstanceOf(Result.Success::class.java)
            val breakdown = (result as Result.Success).data
            assertThat(breakdown.volumes).hasSize(1)
            assertThat(breakdown.volumes.single().categories).isEmpty()
            // Used/free is still available from the volume itself.
            assertThat(
                breakdown.volumes
                    .single()
                    .volume.usedBytes
            ).isEqualTo(600L)
        }

    @Test
    fun `surfaces a volume enumeration failure as an Io error`() =
        runTest {
            val storage = FakeStorage(flow { throw IOException("storage manager blew up") })
            val media = FakeMedia(sizes = Result.Success(emptyMap()))

            val result = GetStorageBreakdownUseCase(storage, media)().first()

            assertThat(result).isInstanceOf(Result.Error::class.java)
            assertThat((result as Result.Error).error).isInstanceOf(OperationError.Io::class.java)
        }
}
