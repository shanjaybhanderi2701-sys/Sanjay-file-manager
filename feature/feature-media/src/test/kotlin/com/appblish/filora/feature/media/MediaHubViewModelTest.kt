package com.appblish.filora.feature.media

import app.cash.turbine.test
import com.appblish.filora.core.common.result.OperationError
import com.appblish.filora.core.common.result.Result
import com.appblish.filora.core.domain.model.FileItem
import com.appblish.filora.core.domain.model.MediaCategory
import com.appblish.filora.core.domain.repository.MediaRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [MediaHubViewModel]. Every assertion runs against the [uiState] flow
 * via Turbine so the loading → loaded emission sequence is pinned, not just the final
 * snapshot. They cover the count→tile mapping (FR-6.1), the degrade-but-stay-navigable
 * error path, and the retry re-query. Navigation itself lives on the screen; what the VM
 * owns — and what these tests assert — is the ordered hub/category mapping that drives it.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MediaHubViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeMedia(
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
    fun `load emits loading then maps counts onto the seven hubs in order`() =
        runTest(dispatcher) {
            val counts = mapOf(MediaCategory.Images to 3, MediaCategory.Video to 9)
            val vm = MediaHubViewModel(FakeMedia(Result.Success(counts)))

            vm.uiState.test {
                assertThat(awaitItem().isLoading).isTrue() // initial loading seed

                val loaded = awaitItem()
                assertThat(loaded.isLoading).isFalse()
                assertThat(loaded.errorMessageRes).isNull()
                assertThat(loaded.tiles.map { it.hub })
                    .containsExactlyElementsIn(CategoryHub.ordered)
                    .inOrder()
                assertThat(loaded.tiles.first { it.hub == CategoryHub.Images }.count).isEqualTo(3)
                assertThat(loaded.tiles.first { it.hub == CategoryHub.Video }.count).isEqualTo(9)
                // Categories absent from the map resolve to a zero count, not a missing tile.
                assertThat(loaded.tiles.first { it.hub == CategoryHub.Audio }.count).isEqualTo(0)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `a load failure still publishes the hubs with zero counts plus an error`() =
        runTest(dispatcher) {
            val vm = MediaHubViewModel(FakeMedia(Result.Error(OperationError.PermissionDenied())))

            vm.uiState.test {
                assertThat(awaitItem().isLoading).isTrue()

                val errored = awaitItem()
                assertThat(errored.isLoading).isFalse()
                assertThat(errored.tiles).hasSize(CategoryHub.ordered.size)
                assertThat(errored.tiles.all { it.count == 0 }).isTrue()
                assertThat(errored.errorMessageRes).isEqualTo(R.string.media_error_permission)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `refresh re-queries and re-emits the loading then loaded sequence`() =
        runTest(dispatcher) {
            val repo = FakeMedia(Result.Success(mapOf(MediaCategory.Audio to 2)))
            val vm = MediaHubViewModel(repo)

            vm.uiState.test {
                awaitItem() // loading seed
                awaitItem() // first load

                vm.refresh()
                assertThat(awaitItem().isLoading).isTrue() // refresh flips loading back on

                val reloaded = awaitItem()
                assertThat(reloaded.isLoading).isFalse()
                assertThat(reloaded.tiles.first { it.hub == CategoryHub.Audio }.count).isEqualTo(2)

                cancelAndIgnoreRemainingEvents()
            }

            assertThat(repo.queries).isEqualTo(2)
        }
}
