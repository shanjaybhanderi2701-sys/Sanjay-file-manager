package com.appblish.filora.feature.media

import com.appblish.filora.core.common.result.OperationError
import com.appblish.filora.core.common.result.Result
import com.appblish.filora.core.domain.model.FileItem
import com.appblish.filora.core.domain.model.MediaCategory
import com.appblish.filora.core.domain.repository.FavoritesRepository
import com.appblish.filora.core.domain.repository.MediaRepository
import com.appblish.filora.core.domain.usecase.ObserveFavoritesUseCase
import com.appblish.filora.core.domain.usecase.ToggleFavoriteUseCase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MediaCategoryDetailViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun file(name: String) =
        FileItem(
            name = name,
            path = "content://media/external/file/$name",
            isDirectory = false,
            sizeBytes = 10,
            lastModifiedEpochMillis = 0,
        )

    private class FakeRepository(
        private val flow: Flow<Result<List<FileItem>>>,
    ) : MediaRepository {
        var observedCategories = mutableListOf<MediaCategory>()

        override fun observeCategory(category: MediaCategory): Flow<Result<List<FileItem>>> {
            observedCategories.add(category)
            return flow
        }

        override suspend fun categoryCounts(): Result<Map<MediaCategory, Int>> = Result.Success(emptyMap())

        override suspend fun categorySizes(): Result<Map<MediaCategory, Long>> = Result.Success(emptyMap())
    }

    /** In-memory favorites keyed by path, exposed as an observable set (FR-9.1). */
    private class FakeFavoritesRepository : FavoritesRepository {
        val favorites = MutableStateFlow<List<FileItem>>(emptyList())

        override fun observeFavorites(): Flow<List<FileItem>> = favorites

        override fun observeRecents(limit: Int): Flow<List<FileItem>> = favorites.map { emptyList() }

        override suspend fun addFavorite(item: FileItem) {
            favorites.update { current -> if (current.any { it.path == item.path }) current else current + item }
        }

        override suspend fun removeFavorite(path: String) {
            favorites.update { current -> current.filterNot { it.path == path } }
        }

        override suspend fun recordRecent(item: FileItem) = Unit
    }

    private fun viewModel(
        repo: MediaRepository,
        favorites: FakeFavoritesRepository = FakeFavoritesRepository(),
    ) = MediaCategoryDetailViewModel(
        repo,
        ToggleFavoriteUseCase(favorites),
        ObserveFavoritesUseCase(favorites),
    )

    @Test
    fun `bind publishes loaded items on success`() =
        runTest(dispatcher) {
            val repo = FakeRepository(flowOf(Result.Success(listOf(file("a.jpg"), file("b.png")))))
            val viewModel = viewModel(repo)

            viewModel.bind(MediaCategory.Images)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.isLoading).isFalse()
            assertThat(state.errorMessageRes).isNull()
            assertThat(state.items.map { it.name }).containsExactly("a.jpg", "b.png").inOrder()
        }

    @Test
    fun `bind surfaces a permission message and clears items on error`() =
        runTest(dispatcher) {
            val repo =
                FakeRepository(
                    flowOf(Result.Error(OperationError.PermissionDenied())),
                )
            val viewModel = viewModel(repo)

            viewModel.bind(MediaCategory.Video)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.isLoading).isFalse()
            assertThat(state.items).isEmpty()
            assertThat(state.errorMessageRes).isEqualTo(R.string.media_detail_error_permission)
        }

    @Test
    fun `re-binding the same category does not re-subscribe`() =
        runTest(dispatcher) {
            val repo = FakeRepository(MutableStateFlow(Result.Success(emptyList())))
            val viewModel = viewModel(repo)

            viewModel.bind(MediaCategory.Audio)
            advanceUntilIdle()
            viewModel.bind(MediaCategory.Audio)
            advanceUntilIdle()

            assertThat(repo.observedCategories).containsExactly(MediaCategory.Audio)
        }

    @Test
    fun `binding a new category re-subscribes`() =
        runTest(dispatcher) {
            val repo = FakeRepository(MutableStateFlow(Result.Success(emptyList())))
            val viewModel = viewModel(repo)

            viewModel.bind(MediaCategory.Audio)
            advanceUntilIdle()
            viewModel.bind(MediaCategory.Documents)
            advanceUntilIdle()

            assertThat(repo.observedCategories)
                .containsExactly(MediaCategory.Audio, MediaCategory.Documents)
                .inOrder()
        }

    @Test
    fun `toggling favorite pins then unpins, reflected in favoritePaths`() =
        runTest(dispatcher) {
            val favorites = FakeFavoritesRepository()
            val repo = FakeRepository(flowOf(Result.Success(listOf(file("a.jpg")))))
            val viewModel = viewModel(repo, favorites)
            viewModel.bind(MediaCategory.Images)
            advanceUntilIdle()
            val item = file("a.jpg")

            viewModel.toggleFavorite(item)
            advanceUntilIdle()
            assertThat(viewModel.uiState.value.favoritePaths).contains(item.path)

            viewModel.toggleFavorite(item)
            advanceUntilIdle()
            assertThat(viewModel.uiState.value.favoritePaths).doesNotContain(item.path)
            assertThat(favorites.favorites.value).isEmpty()
        }
}
