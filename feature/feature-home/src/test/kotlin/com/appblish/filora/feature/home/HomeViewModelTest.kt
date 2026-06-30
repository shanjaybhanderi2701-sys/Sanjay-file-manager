package com.appblish.filora.feature.home

import com.appblish.filora.core.common.result.OperationError
import com.appblish.filora.core.common.result.Result
import com.appblish.filora.core.domain.model.FileItem
import com.appblish.filora.core.domain.model.MediaCategory
import com.appblish.filora.core.domain.repository.MediaAccess
import com.appblish.filora.core.domain.repository.MediaRepository
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

/**
 * Unit tests for [HomeViewModel] — the M4 T4.6 integration surface. They pin the
 * permission gate (no access → prompt, never a MediaStore query), the loaded-count
 * path, the error path, and the `onResume` re-query that turns the prompt into
 * counts once access is granted.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeAccess(
        var granted: Boolean
    ) : MediaAccess {
        override fun hasReadAccess(): Boolean = granted
    }

    private class FakeRepository(
        private val counts: Result<Map<MediaCategory, Int>>,
    ) : MediaRepository {
        var queries = 0

        override fun observeCategory(category: MediaCategory): Flow<Result<List<FileItem>>> =
            flowOf(Result.Success(emptyList()))

        override suspend fun categoryCounts(): Result<Map<MediaCategory, Int>> {
            queries++
            return counts
        }

        override suspend fun categorySizes(): Result<Map<MediaCategory, Long>> = Result.Success(emptyMap())
    }

    @Test
    fun `without access it prompts and never queries`() =
        runTest(dispatcher) {
            val repo = FakeRepository(Result.Success(emptyMap()))
            val viewModel = HomeViewModel(repo, FakeAccess(granted = false))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.permissionRequired).isTrue()
            assertThat(state.isLoading).isFalse()
            assertThat(state.categoryCounts).isEmpty()
            assertThat(repo.queries).isEqualTo(0)
        }

    @Test
    fun `with access it loads category counts`() =
        runTest(dispatcher) {
            val counts = mapOf(MediaCategory.Images to 3, MediaCategory.Audio to 0)
            val viewModel = HomeViewModel(FakeRepository(Result.Success(counts)), FakeAccess(true))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.permissionRequired).isFalse()
            assertThat(state.isLoading).isFalse()
            assertThat(state.errorMessage).isNull()
            assertThat(state.categoryCounts).containsExactlyEntriesIn(counts)
        }

    @Test
    fun `a count failure surfaces an error and clears counts`() =
        runTest(dispatcher) {
            val repo = FakeRepository(Result.Error(OperationError.Io(RuntimeException("boom"))))
            val viewModel = HomeViewModel(repo, FakeAccess(true))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.permissionRequired).isFalse()
            assertThat(state.categoryCounts).isEmpty()
            assertThat(state.errorMessage).isNotNull()
        }

    @Test
    fun `refresh after a grant replaces the prompt with counts`() =
        runTest(dispatcher) {
            val counts = mapOf(MediaCategory.Video to 2)
            val access = FakeAccess(granted = false)
            val viewModel = HomeViewModel(FakeRepository(Result.Success(counts)), access)
            advanceUntilIdle()
            assertThat(viewModel.uiState.value.permissionRequired).isTrue()

            // Simulate the user granting access in settings, then returning (onResume).
            access.granted = true
            viewModel.refresh()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.permissionRequired).isFalse()
            assertThat(state.categoryCounts).containsExactlyEntriesIn(counts)
        }
}
