package com.appblish.filora.feature.browser

import com.appblish.filora.core.common.result.OperationError
import com.appblish.filora.core.common.result.Result
import com.appblish.filora.core.domain.model.DeleteOutcome
import com.appblish.filora.core.domain.model.FileItem
import com.appblish.filora.core.domain.model.SortOrder
import com.appblish.filora.core.domain.model.UserPreferences
import com.appblish.filora.core.domain.model.ViewLayout
import com.appblish.filora.core.domain.repository.FileRepository
import com.appblish.filora.core.domain.repository.SettingsRepository
import com.appblish.filora.core.domain.usecase.ListDirectoryUseCase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
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

    private fun visibleNames(vm: BrowserViewModel) = vm.uiState.value.entries.map { it.name }

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
        ) = error("unused")

        override suspend fun rename(
            path: String,
            newName: String,
        ) = error("unused")

        override suspend fun delete(
            paths: List<String>,
            toTrash: Boolean,
        ): Result<DeleteOutcome> = error("unused")

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

    private fun viewModel(
        snapshot: Result<List<FileItem>> = Result.Success(emptyList()),
        prefs: UserPreferences = UserPreferences.Default,
    ): Triple<BrowserViewModel, FakeFileRepository, FakeSettingsRepository> {
        val repo = FakeFileRepository().apply { this.snapshot = snapshot }
        val settings = FakeSettingsRepository(prefs)
        val vm = BrowserViewModel(ListDirectoryUseCase(repo), settings)
        return Triple(vm, repo, settings)
    }

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
}
