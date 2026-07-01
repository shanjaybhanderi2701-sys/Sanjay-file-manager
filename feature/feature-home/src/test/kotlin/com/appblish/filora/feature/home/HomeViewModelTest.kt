package com.appblish.filora.feature.home

import com.appblish.filora.core.common.result.OperationError
import com.appblish.filora.core.common.result.Result
import com.appblish.filora.core.domain.model.FileItem
import com.appblish.filora.core.domain.model.MediaCategory
import com.appblish.filora.core.domain.model.StorageVolume
import com.appblish.filora.core.domain.repository.FavoritesRepository
import com.appblish.filora.core.domain.repository.MediaAccess
import com.appblish.filora.core.domain.repository.MediaRepository
import com.appblish.filora.core.domain.repository.StorageRepository
import com.appblish.filora.core.domain.usecase.ObserveFavoritesUseCase
import com.appblish.filora.core.domain.usecase.ObserveRecentsUseCase
import com.appblish.filora.core.domain.usecase.ObserveStorageVolumesUseCase
import com.appblish.filora.core.domain.usecase.ToggleFavoriteUseCase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
 * Unit tests for [HomeViewModel] — the M4 T4.6 integration surface plus the M6 T6.2
 * favorites/recents wiring. They pin the permission gate (no access → prompt, never a
 * MediaStore query), the loaded-count path, the error path, the `onResume` re-query
 * that turns the prompt into counts once access is granted, and the persisted
 * favorites/recents streams that flow into state independently of media access.
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
        var granted: Boolean,
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

    /** In-memory [StorageRepository] backing the volumes observe use case under test. */
    private class FakeStorage : StorageRepository {
        val volumes = MutableStateFlow<List<StorageVolume>>(emptyList())

        override fun observeVolumes(): Flow<List<StorageVolume>> = volumes

        override suspend fun getVolume(id: String): Result<StorageVolume> =
            volumes.value.firstOrNull { it.id == id }?.let { Result.Success(it) }
                ?: Result.Error(OperationError.NotFound())

        override suspend fun largestFiles(
            rootPath: String,
            limit: Int,
        ): Result<List<FileItem>> = Result.Success(emptyList())
    }

    /** In-memory [FavoritesRepository] backing the two observe use cases under test. */
    private class FakeFavorites : FavoritesRepository {
        val favorites = MutableStateFlow<List<FileItem>>(emptyList())
        val recents = MutableStateFlow<List<FileItem>>(emptyList())

        override fun observeFavorites(): Flow<List<FileItem>> = favorites

        override fun observeRecents(limit: Int): Flow<List<FileItem>> = recents

        override suspend fun addFavorite(item: FileItem) {
            favorites.value = favorites.value + item
        }

        override suspend fun removeFavorite(path: String) {
            favorites.value = favorites.value.filterNot { it.path == path }
        }

        override suspend fun recordRecent(item: FileItem) {
            recents.value = listOf(item) + recents.value.filterNot { it.path == item.path }
        }
    }

    private fun viewModel(
        repository: MediaRepository,
        access: MediaAccess,
        favorites: FavoritesRepository = FakeFavorites(),
        storage: StorageRepository = FakeStorage(),
    ) = HomeViewModel(
        mediaRepository = repository,
        mediaAccess = access,
        toggleFavorite = ToggleFavoriteUseCase(favorites),
        observeFavorites = ObserveFavoritesUseCase(favorites),
        observeRecents = ObserveRecentsUseCase(favorites),
        observeStorageVolumes = ObserveStorageVolumesUseCase(storage),
    )

    private fun volume(id: String) =
        StorageVolume(
            id = id,
            label = id,
            rootPath = "/storage/$id",
            totalBytes = 100L,
            availableBytes = 40L,
            isRemovable = false,
            isPrimary = true,
        )

    private fun file(path: String) =
        FileItem(
            name = path.substringAfterLast('/'),
            path = path,
            isDirectory = false,
            sizeBytes = 0L,
            lastModifiedEpochMillis = 0L
        )

    @Test
    fun `without access it prompts and never queries`() =
        runTest(dispatcher) {
            val repo = FakeRepository(Result.Success(emptyMap()))
            val vm = viewModel(repo, FakeAccess(granted = false))
            advanceUntilIdle()

            val state = vm.uiState.value
            assertThat(state.permissionRequired).isTrue()
            assertThat(state.isLoading).isFalse()
            assertThat(state.categoryCounts).isEmpty()
            assertThat(repo.queries).isEqualTo(0)
        }

    @Test
    fun `with access it loads category counts`() =
        runTest(dispatcher) {
            val counts = mapOf(MediaCategory.Images to 3, MediaCategory.Audio to 0)
            val vm = viewModel(FakeRepository(Result.Success(counts)), FakeAccess(true))
            advanceUntilIdle()

            val state = vm.uiState.value
            assertThat(state.permissionRequired).isFalse()
            assertThat(state.isLoading).isFalse()
            assertThat(state.errorMessageRes).isNull()
            assertThat(state.categoryCounts).containsExactlyEntriesIn(counts)
        }

    @Test
    fun `a count failure surfaces an error and clears counts`() =
        runTest(dispatcher) {
            val repo = FakeRepository(Result.Error(OperationError.Io(RuntimeException("boom"))))
            val vm = viewModel(repo, FakeAccess(true))
            advanceUntilIdle()

            val state = vm.uiState.value
            assertThat(state.permissionRequired).isFalse()
            assertThat(state.categoryCounts).isEmpty()
            assertThat(state.errorMessageRes).isNotNull()
        }

    @Test
    fun `refresh after a grant replaces the prompt with counts`() =
        runTest(dispatcher) {
            val counts = mapOf(MediaCategory.Video to 2)
            val access = FakeAccess(granted = false)
            val vm = viewModel(FakeRepository(Result.Success(counts)), access)
            advanceUntilIdle()
            assertThat(vm.uiState.value.permissionRequired).isTrue()

            // Simulate the user granting access in settings, then returning (onResume).
            access.granted = true
            vm.refresh()
            advanceUntilIdle()

            val state = vm.uiState.value
            assertThat(state.permissionRequired).isFalse()
            assertThat(state.categoryCounts).containsExactlyEntriesIn(counts)
        }

    @Test
    fun `persisted favorites and recents flow into state without media access`() =
        runTest(dispatcher) {
            val favorites = FakeFavorites()
            favorites.favorites.value = listOf(file("/a/pinned.txt"))
            favorites.recents.value = listOf(file("/a/opened.txt"))

            val vm = viewModel(FakeRepository(Result.Success(emptyMap())), FakeAccess(granted = false), favorites)
            advanceUntilIdle()

            val state = vm.uiState.value
            assertThat(state.permissionRequired).isTrue()
            assertThat(state.favorites.map { it.path }).containsExactly("/a/pinned.txt")
            assertThat(state.recents.map { it.path }).containsExactly("/a/opened.txt")
        }

    @Test
    fun `storage volumes flow into state without media access`() =
        runTest(dispatcher) {
            val storage = FakeStorage()
            storage.volumes.value = listOf(volume("internal"))

            val vm =
                viewModel(
                    FakeRepository(Result.Success(emptyMap())),
                    FakeAccess(granted = false),
                    storage = storage,
                )
            advanceUntilIdle()

            assertThat(
                vm.uiState.value.volumes
                    .map { it.id }
            ).containsExactly("internal")
        }

    @Test
    fun `volume mount re-emits into state`() =
        runTest(dispatcher) {
            val storage = FakeStorage()
            val vm = viewModel(FakeRepository(Result.Success(emptyMap())), FakeAccess(true), storage = storage)
            advanceUntilIdle()
            assertThat(vm.uiState.value.volumes).isEmpty()

            storage.volumes.value = listOf(volume("internal"), volume("sdcard"))
            advanceUntilIdle()

            assertThat(
                vm.uiState.value.volumes
                    .map { it.id }
            ).containsExactly("internal", "sdcard").inOrder()
        }

    @Test
    fun `favorites updates re-emit into state`() =
        runTest(dispatcher) {
            val favorites = FakeFavorites()
            val vm = viewModel(FakeRepository(Result.Success(emptyMap())), FakeAccess(true), favorites)
            advanceUntilIdle()
            assertThat(vm.uiState.value.favorites).isEmpty()

            favorites.addFavorite(file("/b/star.txt"))
            advanceUntilIdle()

            assertThat(
                vm.uiState.value.favorites
                    .map { it.path }
            ).containsExactly("/b/star.txt")
        }

    @Test
    fun `unpinFavorite removes the item from the favorites strip`() =
        runTest(dispatcher) {
            val favorites = FakeFavorites()
            favorites.favorites.value = listOf(file("/a/pinned.txt"))
            val vm = viewModel(FakeRepository(Result.Success(emptyMap())), FakeAccess(true), favorites)
            advanceUntilIdle()
            assertThat(
                vm.uiState.value.favorites
                    .map { it.path }
            ).containsExactly("/a/pinned.txt")

            vm.unpinFavorite(file("/a/pinned.txt"))
            advanceUntilIdle()

            assertThat(vm.uiState.value.favorites).isEmpty()
            assertThat(favorites.favorites.value).isEmpty()
        }
}
