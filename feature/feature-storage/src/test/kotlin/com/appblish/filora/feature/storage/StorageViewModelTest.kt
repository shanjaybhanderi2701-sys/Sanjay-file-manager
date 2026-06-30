package com.appblish.filora.feature.storage

import com.appblish.filora.core.common.result.OperationError
import com.appblish.filora.core.common.result.Result
import com.appblish.filora.core.domain.model.FileItem
import com.appblish.filora.core.domain.model.MediaCategory
import com.appblish.filora.core.domain.model.StorageVolume
import com.appblish.filora.core.domain.repository.MediaRepository
import com.appblish.filora.core.domain.repository.StorageRepository
import com.appblish.filora.core.domain.usecase.GetStorageBreakdownUseCase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class StorageViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun volume() =
        StorageVolume(
            id = "internal",
            label = "Internal storage",
            rootPath = "/storage/internal",
            totalBytes = 1_000,
            availableBytes = 400,
            isRemovable = false,
            isPrimary = true,
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
    ) : MediaRepository {
        override fun observeCategory(category: MediaCategory): Flow<Result<List<FileItem>>> =
            flowOf(Result.Success(emptyList()))

        override suspend fun categoryCounts(): Result<Map<MediaCategory, Int>> =
            Result.Success(mapOf(MediaCategory.Images to 2))

        override suspend fun categorySizes(): Result<Map<MediaCategory, Long>> = sizes
    }

    private fun viewModel(
        storage: StorageRepository,
        media: MediaRepository,
    ) = StorageViewModel(GetStorageBreakdownUseCase(storage, media))

    @Test
    fun `publishes the breakdown on success`() =
        runTest(dispatcher) {
            val vm =
                viewModel(
                    FakeStorage(flowOf(listOf(volume()))),
                    FakeMedia(Result.Success(mapOf(MediaCategory.Images to 250L))),
                )
            advanceUntilIdle()

            val state = vm.uiState.value
            assertThat(state.isLoading).isFalse()
            assertThat(state.errorMessageRes).isNull()
            assertThat(state.breakdown?.volumes).hasSize(1)
            val only = state.breakdown?.volumes?.single()
            assertThat(only?.categories?.single()?.category).isEqualTo(MediaCategory.Images)
        }

    @Test
    fun `surfaces an error message when volumes cannot be read`() =
        runTest(dispatcher) {
            val vm =
                viewModel(
                    FakeStorage(flow { throw IOException("boom") }),
                    FakeMedia(Result.Success(emptyMap())),
                )
            advanceUntilIdle()

            val state = vm.uiState.value
            assertThat(state.isLoading).isFalse()
            assertThat(state.errorMessageRes).isNotNull()
        }

    @Test
    fun `still renders used and free when media access is denied`() =
        runTest(dispatcher) {
            val vm =
                viewModel(
                    FakeStorage(flowOf(listOf(volume()))),
                    FakeMedia(Result.Error(OperationError.PermissionDenied())),
                )
            advanceUntilIdle()

            val state = vm.uiState.value
            assertThat(state.errorMessageRes).isNull()
            val only = state.breakdown?.volumes?.single()
            assertThat(only?.categories).isEmpty()
            assertThat(only?.volume?.usedBytes).isEqualTo(600L)
        }
}
