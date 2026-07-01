package com.appblish.filora.feature.storage

import app.cash.turbine.test
import com.appblish.filora.core.common.result.OperationError
import com.appblish.filora.core.common.result.Result
import com.appblish.filora.core.common.result.asError
import com.appblish.filora.core.common.result.asSuccess
import com.appblish.filora.core.domain.model.FileItem
import com.appblish.filora.core.domain.model.StorageVolume
import com.appblish.filora.core.domain.repository.StorageRepository
import com.appblish.filora.core.domain.usecase.GetLargestFilesUseCase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LargestFilesViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun file(
        name: String,
        size: Long = 100,
    ) = FileItem(
        name = name,
        path = "/storage/internal/$name",
        isDirectory = false,
        sizeBytes = size,
        lastModifiedEpochMillis = 0,
    )

    private class FakeStorage(
        private val result: Result<List<FileItem>>,
    ) : StorageRepository {
        var scans = 0

        override fun observeVolumes(): Flow<List<StorageVolume>> =
            flowOf(
                listOf(
                    StorageVolume(
                        id = "internal",
                        label = "Internal storage",
                        rootPath = "/storage/internal",
                        totalBytes = 1_000,
                        availableBytes = 400,
                        isRemovable = false,
                        isPrimary = true,
                    ),
                ),
            )

        override suspend fun getVolume(id: String): Result<StorageVolume> = throw UnsupportedOperationException()

        override suspend fun largestFiles(
            rootPath: String,
            limit: Int,
        ): Result<List<FileItem>> {
            scans++
            return result
        }
    }

    private fun viewModel(storage: StorageRepository) = LargestFilesViewModel(GetLargestFilesUseCase(storage))

    @Test
    fun `load publishes the largest files`() =
        runTest(dispatcher) {
            val vm = viewModel(FakeStorage(listOf(file("big.mp4", 900), file("doc.pdf", 100)).asSuccess()))

            vm.load(volumeId = null)
            advanceUntilIdle()

            val state = vm.uiState.value
            assertThat(state.isLoading).isFalse()
            assertThat(state.errorMessageRes).isNull()
            assertThat(state.files.map { it.name }).containsExactly("big.mp4", "doc.pdf").inOrder()
        }

    @Test
    fun `load surfaces an error message on failure`() =
        runTest(dispatcher) {
            val vm = viewModel(FakeStorage(OperationError.PermissionDenied().asError()))

            vm.load(volumeId = null)
            advanceUntilIdle()

            val state = vm.uiState.value
            assertThat(state.isLoading).isFalse()
            assertThat(state.files).isEmpty()
            assertThat(state.errorMessageRes).isEqualTo(R.string.storage_largest_error_permission)
        }

    @Test
    fun `re-binding the same volume does not rescan`() =
        runTest(dispatcher) {
            val storage = FakeStorage(emptyList<FileItem>().asSuccess())
            val vm = viewModel(storage)

            vm.load(volumeId = null)
            advanceUntilIdle()
            vm.load(volumeId = null)
            advanceUntilIdle()

            assertThat(storage.scans).isEqualTo(1)
        }

    @Test
    fun `load emits the loading then content sequence through the state flow`() =
        runTest(dispatcher) {
            val vm = viewModel(FakeStorage(listOf(file("big.mp4", 900), file("doc.pdf", 100)).asSuccess()))

            vm.uiState.test {
                assertThat(awaitItem().isLoading).isTrue() // initial loading seed

                vm.load(volumeId = null)
                val content = awaitItem()
                assertThat(content.isLoading).isFalse()
                assertThat(content.errorMessageRes).isNull()
                assertThat(content.files.map { it.name }).containsExactly("big.mp4", "doc.pdf").inOrder()

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `load emits an error message through the state flow on failure`() =
        runTest(dispatcher) {
            val vm = viewModel(FakeStorage(OperationError.PermissionDenied().asError()))

            vm.uiState.test {
                assertThat(awaitItem().isLoading).isTrue()

                vm.load(volumeId = null)
                val errored = awaitItem()
                assertThat(errored.isLoading).isFalse()
                assertThat(errored.files).isEmpty()
                assertThat(errored.errorMessageRes).isEqualTo(R.string.storage_largest_error_permission)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `onDeleted drops the file from the list`() =
        runTest(dispatcher) {
            val target = file("big.mp4", 900)
            val vm = viewModel(FakeStorage(listOf(target, file("doc.pdf", 100)).asSuccess()))

            vm.load(volumeId = null)
            advanceUntilIdle()
            vm.onDeleted(target)

            assertThat(
                vm.uiState.value.files
                    .map { it.name }
            ).containsExactly("doc.pdf")
        }
}
