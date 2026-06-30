package com.appblish.filora.feature.browser

import com.appblish.filora.core.common.result.OperationError
import com.appblish.filora.core.common.result.Result
import com.appblish.filora.core.domain.model.DeleteOutcome
import com.appblish.filora.core.domain.model.FileItem
import com.appblish.filora.core.domain.model.SortOrder
import com.appblish.filora.core.domain.model.UserPreferences
import com.appblish.filora.core.domain.model.ViewLayout
import com.appblish.filora.core.domain.repository.FavoritesRepository
import com.appblish.filora.core.domain.repository.FileRepository
import com.appblish.filora.core.domain.repository.SettingsRepository
import com.appblish.filora.core.domain.usecase.CreateFolderUseCase
import com.appblish.filora.core.domain.usecase.DeleteUseCase
import com.appblish.filora.core.domain.usecase.ListDirectoryUseCase
import com.appblish.filora.core.domain.usecase.ObserveFavoritesUseCase
import com.appblish.filora.core.domain.usecase.RenameUseCase
import com.appblish.filora.core.domain.usecase.ToggleFavoriteUseCase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
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
class BrowserViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() = Dispatchers.resetMain()

    private fun visibleNames(vm: BrowserViewModel) =
        vm.uiState.value.entries
            .map { it.name }

    private fun file(
        name: String,
        isDirectory: Boolean = false,
        size: Long = 0,
    ) = FileItem(
        name = name,
        path = "/root/$name",
        isDirectory = isDirectory,
        sizeBytes = size,
        lastModifiedEpochMillis = 0,
        isHidden = name.startsWith("."),
    )

    /** Fake that records the sort each listing was requested with and returns a fixed snapshot. */
    private class FakeFileRepository : FileRepository {
        var snapshot: Result<List<FileItem>> = Result.Success(emptyList())
        val requestedSorts = mutableListOf<SortOrder>()
        var createResult: Result<FileItem> = Result.Error(OperationError.Unknown())
        var renameResult: Result<FileItem> = Result.Error(OperationError.Unknown())
        var deleteResult: Result<DeleteOutcome> = Result.Success(DeleteOutcome(0, false))
        val deletedPaths = mutableListOf<List<String>>()

        override fun listDirectory(
            path: String,
            sortOrder: SortOrder,
        ): Flow<Result<List<FileItem>>> =
            flow {
                requestedSorts.add(sortOrder)
                emit(snapshot)
            }

        override suspend fun getFile(path: String) = error("unused")

        override suspend fun createFolder(
            parentPath: String,
            name: String,
        ) = createResult

        override suspend fun rename(
            path: String,
            newName: String,
        ) = renameResult

        override suspend fun delete(
            paths: List<String>,
            toTrash: Boolean,
        ): Result<DeleteOutcome> {
            deletedPaths.add(paths)
            return deleteResult
        }

        override suspend fun copy(
            sourcePath: String,
            destinationDir: String,
            destinationName: String,
            overwrite: Boolean,
        ) = error("unused")
    }

    private class FakeSettingsRepository(
        initial: UserPreferences,
    ) : SettingsRepository {
        val state = MutableStateFlow(initial)
        override val preferences = state

        override suspend fun setThemeMode(mode: com.appblish.filora.core.domain.model.ThemeMode) = Unit

        override suspend fun setUseDynamicColor(enabled: Boolean) = Unit

        override suspend fun setShowHiddenFiles(enabled: Boolean) {
            state.update { it.copy(showHiddenFiles = enabled) }
        }

        override suspend fun setDefaultViewLayout(layout: ViewLayout) {
            state.update { it.copy(defaultViewLayout = layout) }
        }

        override suspend fun setDefaultSortOrder(sortOrder: SortOrder) {
            state.update { it.copy(defaultSortOrder = sortOrder) }
        }
    }

    /** In-memory favorites keyed by path, exposed as an observable set (FR-9.1). */
    private class FakeFavoritesRepository : FavoritesRepository {
        val favorites = MutableStateFlow<List<FileItem>>(emptyList())

        override fun observeFavorites(): Flow<List<FileItem>> = favorites

        override fun observeRecents(limit: Int): Flow<List<FileItem>> =
            favorites.map { emptyList() }

        override suspend fun addFavorite(item: FileItem) {
            favorites.update { current -> if (current.any { it.path == item.path }) current else current + item }
        }

        override suspend fun removeFavorite(path: String) {
            favorites.update { current -> current.filterNot { it.path == path } }
        }

        override suspend fun recordRecent(item: FileItem) = Unit
    }

    private fun viewModel(
        snapshot: Result<List<FileItem>> = Result.Success(emptyList()),
        prefs: UserPreferences = UserPreferences.Default,
        favorites: FakeFavoritesRepository = FakeFavoritesRepository(),
    ): BrowserFixture {
        val repo = FakeFileRepository().apply { this.snapshot = snapshot }
        val settings = FakeSettingsRepository(prefs)
        val vm =
            BrowserViewModel(
                ListDirectoryUseCase(repo),
                settings,
                ToggleFavoriteUseCase(favorites),
                ObserveFavoritesUseCase(favorites),
                CreateFolderUseCase(repo),
                RenameUseCase(repo),
                DeleteUseCase(repo),
            )
        return BrowserFixture(vm, repo, settings, favorites)
    }

    private data class BrowserFixture(
        val vm: BrowserViewModel,
        val repo: FakeFileRepository,
        val settings: FakeSettingsRepository,
        val favorites: FakeFavoritesRepository,
    )

    @Test
    fun `successful listing moves to content`() =
        runTest(dispatcher) {
            val (vm, _, _) = viewModel(Result.Success(listOf(file("Docs", isDirectory = true), file("a.txt"))))
            vm.bindLocation("/root")
            advanceUntilIdle()

            val state = vm.uiState.value
            assertThat(state.isContent).isTrue()
            assertThat(visibleNames(vm)).containsExactly("Docs", "a.txt")
        }

    @Test
    fun `empty listing moves to empty state`() =
        runTest(dispatcher) {
            val (vm, _, _) = viewModel(Result.Success(emptyList()))
            vm.bindLocation("/root")
            advanceUntilIdle()

            assertThat(vm.uiState.value.isEmpty).isTrue()
        }

    @Test
    fun `error listing surfaces an error message`() =
        runTest(dispatcher) {
            val (vm, _, _) = viewModel(Result.Error(OperationError.PermissionDenied()))
            vm.bindLocation("/root")
            advanceUntilIdle()

            val state = vm.uiState.value
            assertThat(state.isError).isTrue()
            assertThat(state.errorMessageRes).isNotNull()
        }

    @Test
    fun `hidden files are filtered until shown, without re-reading`() =
        runTest(dispatcher) {
            val (vm, repo, _) = viewModel(Result.Success(listOf(file("visible.txt"), file(".secret"))))
            vm.bindLocation("/root")
            advanceUntilIdle()
            assertThat(visibleNames(vm)).containsExactly("visible.txt")
            val readsBefore = repo.requestedSorts.size

            vm.setShowHidden(true)
            advanceUntilIdle()

            assertThat(visibleNames(vm)).containsExactly("visible.txt", ".secret")
            assertThat(repo.requestedSorts.size).isEqualTo(readsBefore) // re-filter only, no re-read
        }

    @Test
    fun `toggling layout flips and persists`() =
        runTest(dispatcher) {
            val (vm, _, settings) = viewModel(Result.Success(listOf(file("a.txt"))))
            vm.bindLocation("/root")
            advanceUntilIdle()
            assertThat(vm.uiState.value.layout).isEqualTo(ViewLayout.List)

            vm.toggleLayout()
            advanceUntilIdle()

            assertThat(vm.uiState.value.layout).isEqualTo(ViewLayout.Grid)
            assertThat(settings.state.value.defaultViewLayout).isEqualTo(ViewLayout.Grid)
        }

    @Test
    fun `selecting a new sort key re-reads with that order, ascending`() =
        runTest(dispatcher) {
            val (vm, repo, _) = viewModel(Result.Success(listOf(file("a.txt"))))
            vm.bindLocation("/root")
            advanceUntilIdle()

            vm.setSortBy(SortOrder.By.Size)
            advanceUntilIdle()

            assertThat(vm.uiState.value.sortOrder.by).isEqualTo(SortOrder.By.Size)
            assertThat(vm.uiState.value.sortOrder.ascending).isTrue()
            assertThat(repo.requestedSorts.last().by).isEqualTo(SortOrder.By.Size)
        }

    @Test
    fun `re-selecting the active sort key flips its direction`() =
        runTest(dispatcher) {
            val (vm, _, _) = viewModel(Result.Success(listOf(file("a.txt"))))
            vm.bindLocation("/root")
            advanceUntilIdle()

            vm.setSortBy(SortOrder.By.Name) // already the default key
            advanceUntilIdle()

            assertThat(vm.uiState.value.sortOrder.by).isEqualTo(SortOrder.By.Name)
            assertThat(vm.uiState.value.sortOrder.ascending).isFalse()
        }

    @Test
    fun `refresh issues another directory read`() =
        runTest(dispatcher) {
            val (vm, repo, _) = viewModel(Result.Success(listOf(file("a.txt"))))
            vm.bindLocation("/root")
            advanceUntilIdle()
            val readsBefore = repo.requestedSorts.size

            vm.refresh()
            advanceUntilIdle()

            assertThat(repo.requestedSorts.size).isGreaterThan(readsBefore)
            assertThat(vm.uiState.value.isRefreshing).isFalse()
        }

    @Test
    fun `toggling favorite pins then unpins, reflected in favoritePaths`() =
        runTest(dispatcher) {
            val (vm, _, _, favorites) = viewModel(Result.Success(listOf(file("a.txt"))))
            vm.bindLocation("/root")
            advanceUntilIdle()
            val item = file("a.txt")
            assertThat(vm.uiState.value.favoritePaths).doesNotContain(item.path)

            vm.toggleFavorite(item)
            advanceUntilIdle()
            assertThat(vm.uiState.value.favoritePaths).contains(item.path)
            assertThat(favorites.favorites.value.map { it.path }).containsExactly(item.path)

            vm.toggleFavorite(item)
            advanceUntilIdle()
            assertThat(vm.uiState.value.favoritePaths).doesNotContain(item.path)
            assertThat(favorites.favorites.value).isEmpty()
        }

    @Test
    fun `pre-existing favorites surface in favoritePaths on bind`() =
        runTest(dispatcher) {
            val seeded = FakeFavoritesRepository().apply { favorites.value = listOf(file("a.txt")) }
            val (vm, _, _, _) = viewModel(Result.Success(listOf(file("a.txt"))), favorites = seeded)
            vm.bindLocation("/root")
            advanceUntilIdle()

            assertThat(vm.uiState.value.favoritePaths).contains("/root/a.txt")
        }

    // ---- Multi-select (T075/T076) -------------------------------------------

    @Test
    fun `toggling an item enters and exits selection mode`() =
        runTest(dispatcher) {
            val (vm, _, _) = viewModel(Result.Success(listOf(file("a.txt"), file("b.txt"))))
            vm.bindLocation("/root")
            advanceUntilIdle()
            val item = vm.uiState.value.entries
                .first()

            vm.toggleSelection(item)
            assertThat(vm.uiState.value.inSelectionMode).isTrue()
            assertThat(vm.uiState.value.selection.count).isEqualTo(1)

            vm.toggleSelection(item)
            assertThat(vm.uiState.value.inSelectionMode).isFalse()
        }

    @Test
    fun `select-all selects every visible entry and clear exits`() =
        runTest(dispatcher) {
            val (vm, _, _) = viewModel(Result.Success(listOf(file("a.txt"), file("Docs", isDirectory = true))))
            vm.bindLocation("/root")
            advanceUntilIdle()

            vm.selectAll()
            assertThat(vm.uiState.value.selection.count).isEqualTo(2)

            vm.clearSelection()
            assertThat(vm.uiState.value.inSelectionMode).isFalse()
        }

    // ---- Operations with in-place updates (T066–T068/T077) ------------------

    @Test
    fun `creating a folder adds it to the list in place`() =
        runTest(dispatcher) {
            val (vm, repo, _) = viewModel(Result.Success(listOf(file("a.txt"))))
            vm.bindLocation("/root")
            advanceUntilIdle()
            val readsBefore = repo.requestedSorts.size
            repo.createResult = Result.Success(file("New", isDirectory = true))

            vm.showNewFolderDialog()
            vm.confirmCreateFolder("New")
            advanceUntilIdle()

            assertThat(visibleNames(vm)).containsExactly("a.txt", "New")
            assertThat(vm.uiState.value.dialog).isNull()
            assertThat(repo.requestedSorts.size).isEqualTo(readsBefore) // in place, no re-read
        }

    @Test
    fun `renaming swaps the entry in place without re-reading`() =
        runTest(dispatcher) {
            val (vm, repo, _) = viewModel(Result.Success(listOf(file("old.txt"))))
            vm.bindLocation("/root")
            advanceUntilIdle()
            val item = vm.uiState.value.entries
                .first()
            val readsBefore = repo.requestedSorts.size
            repo.renameResult = Result.Success(item.copy(name = "new.txt", path = "/root/new.txt"))

            vm.showRenameDialog(item)
            vm.confirmRename("new.txt")
            advanceUntilIdle()

            assertThat(visibleNames(vm)).containsExactly("new.txt")
            assertThat(repo.requestedSorts.size).isEqualTo(readsBefore)
        }

    @Test
    fun `deleting the selection removes the rows in place`() =
        runTest(dispatcher) {
            val (vm, repo, _) = viewModel(Result.Success(listOf(file("a.txt"), file("b.txt"))))
            vm.bindLocation("/root")
            advanceUntilIdle()
            repo.deleteResult = Result.Success(DeleteOutcome(1, true))
            val target = vm.uiState.value.entries
                .first { it.name == "a.txt" }

            vm.toggleSelection(target)
            vm.showDeleteDialog()
            vm.confirmDelete()
            advanceUntilIdle()

            assertThat(visibleNames(vm)).containsExactly("b.txt")
            assertThat(repo.deletedPaths).contains(listOf("/root/a.txt"))
            assertThat(vm.uiState.value.inSelectionMode).isFalse()
            assertThat(vm.uiState.value.messageRes).isNotNull()
        }

    @Test
    fun `a failed create closes the dialog and surfaces a message`() =
        runTest(dispatcher) {
            val (vm, repo, _) = viewModel(Result.Success(listOf(file("a.txt"))))
            vm.bindLocation("/root")
            advanceUntilIdle()
            repo.createResult = Result.Error(OperationError.Conflict())

            vm.showNewFolderDialog()
            vm.confirmCreateFolder("New")
            advanceUntilIdle()

            assertThat(vm.uiState.value.dialog).isNull()
            assertThat(vm.uiState.value.messageRes).isNotNull()
            assertThat(visibleNames(vm)).containsExactly("a.txt")
        }
}
