package com.appblish.filora.core.domain.usecase

import app.cash.turbine.test
import com.appblish.filora.core.common.result.Result
import com.appblish.filora.core.domain.model.FileItem
import com.appblish.filora.core.domain.model.StorageVolume
import com.appblish.filora.core.domain.repository.StorageRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ObserveStorageVolumesUseCaseTest {
    private fun volume(id: String) =
        StorageVolume(
            id = id,
            label = id,
            rootPath = "/storage/$id",
            totalBytes = 1_000,
            availableBytes = 400,
            isRemovable = id != "internal",
            isPrimary = id == "internal",
        )

    /** In-memory volume enumerator that re-emits on mount/unmount. */
    private class FakeStorage : StorageRepository {
        val volumes = MutableStateFlow<List<StorageVolume>>(emptyList())

        override fun observeVolumes(): Flow<List<StorageVolume>> = volumes

        override suspend fun getVolume(id: String): Result<StorageVolume> = throw UnsupportedOperationException()

        override suspend fun largestFiles(
            rootPath: String,
            limit: Int,
        ): Result<List<FileItem>> = throw UnsupportedOperationException()
    }

    @Test
    fun `streams the mounted volumes and re-emits on mount`() =
        runTest {
            val repo = FakeStorage()
            repo.volumes.value = listOf(volume("internal"))

            ObserveStorageVolumesUseCase(repo)().test {
                assertThat(awaitItem().map { it.id }).containsExactly("internal")

                repo.volumes.value = listOf(volume("internal"), volume("sdcard"))
                assertThat(awaitItem().map { it.id })
                    .containsExactly("internal", "sdcard")
                    .inOrder()

                cancelAndIgnoreRemainingEvents()
            }
        }
}
