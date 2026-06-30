package com.appblish.filora.feature.search

import com.appblish.filora.core.domain.model.FileItem
import com.appblish.filora.core.domain.model.FileTypeFilter
import com.appblish.filora.core.domain.model.SearchProgress
import com.appblish.filora.core.domain.model.SearchQuery
import com.appblish.filora.core.domain.repository.SearchRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

private const val KB = 1_024L
private const val MB = 1_048_576L
private const val DAY = 24L * 60 * 60 * 1000
private const val NOW = 1_000_000_000_000L

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
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
        size: Long,
        modified: Long = NOW,
        mime: String? = null,
    ) = FileItem(
        name = name,
        path = "/root/$name",
        isDirectory = false,
        sizeBytes = size,
        lastModifiedEpochMillis = modified,
        mimeType = mime,
    )

    /**
     * Fake that mirrors the real repository's in-walk acceptance ([SearchQuery.accepts]):
     * name-substring AND the AND-combined filter, streamed as Match emissions closed by a
     * Completed terminal. Lets the ViewModel test exercise streaming + filter pass-through
     * without a filesystem.
     */
    private class FakeSearchRepository(
        private val tree: List<FileItem>
    ) : SearchRepository {
        val queries = mutableListOf<SearchQuery>()

        override fun search(query: SearchQuery): Flow<SearchProgress> =
            flow {
                queries.add(query)
                if (query.isBlank || query.rootPath.isNullOrBlank()) {
                    emit(SearchProgress.Completed(matchCount = 0))
                    return@flow
                }
                val needle = query.text.trim()
                var count = 0
                tree.forEach { item ->
                    val nameMatch = needle.isBlank() || item.name.contains(needle, ignoreCase = true)
                    if (nameMatch && query.filter.matches(item)) {
                        count += 1
                        emit(SearchProgress.Match(item, matchCount = count))
                    }
                }
                emit(SearchProgress.Completed(matchCount = count))
            }
    }

    private val sample =
        listOf(
            file("report.jpg", size = 2 * MB),
            file("report.mp3", size = 5 * MB),
            file("report.pdf", size = 500 * KB, modified = NOW - 90 * DAY),
            file("report.zip", size = 200 * MB),
        )

    private fun viewModel(items: List<FileItem> = sample): Pair<SearchViewModel, FakeSearchRepository> {
        val repo = FakeSearchRepository(items)
        val vm = SearchViewModel(repo)
        vm.nowProvider = { NOW }
        vm.bindScope("/root")
        return vm to repo
    }

    @Test
    fun `query streams all name matches`() =
        runTest(dispatcher) {
            val (vm, _) = viewModel()
            vm.onQueryChange("report")
            advanceUntilIdle()

            val state = vm.uiState.value
            assertThat(state.isSearching).isFalse()
            assertThat(state.hasSearched).isTrue()
            assertThat(state.results.map { it.name })
                .containsExactly("report.jpg", "report.mp3", "report.pdf", "report.zip")
        }

    @Test
    fun `blank query clears results and hasSearched`() =
        runTest(dispatcher) {
            val (vm, _) = viewModel()
            vm.onQueryChange("report")
            advanceUntilIdle()
            vm.onQueryChange("   ")
            advanceUntilIdle()

            val state = vm.uiState.value
            assertThat(state.results).isEmpty()
            assertThat(state.hasSearched).isFalse()
            assertThat(state.isEmpty).isFalse()
        }

    @Test
    fun `type filter is passed into the walk and narrows results`() =
        runTest(dispatcher) {
            val (vm, repo) = viewModel()
            vm.onQueryChange("report")
            advanceUntilIdle()

            vm.toggleType(FileTypeFilter.Image)
            advanceUntilIdle()

            assertThat(
                vm.uiState.value.results
                    .map { it.name }
            ).containsExactly("report.jpg")
            assertThat(
                repo.queries
                    .last()
                    .filter.types
            ).containsExactly(FileTypeFilter.Image)
        }

    @Test
    fun `type and size filters combine with AND`() =
        runTest(dispatcher) {
            val (vm, _) = viewModel()
            vm.onQueryChange("report")
            advanceUntilIdle()

            // Audio OR image, AND under 1 MB → nothing (jpg is 2 MB, mp3 is 5 MB).
            vm.toggleType(FileTypeFilter.Image)
            vm.toggleType(FileTypeFilter.Audio)
            vm.selectSize(SizeBucket.Small)
            advanceUntilIdle()
            assertThat(vm.uiState.value.results).isEmpty()

            // Widen size to 1–100 MB → both the 2 MB image and 5 MB audio qualify.
            vm.selectSize(SizeBucket.Medium)
            advanceUntilIdle()
            assertThat(
                vm.uiState.value.results
                    .map { it.name }
            ).containsExactly("report.jpg", "report.mp3")
        }

    @Test
    fun `date filter excludes files modified before the window`() =
        runTest(dispatcher) {
            val (vm, _) = viewModel()
            vm.onQueryChange("report")
            advanceUntilIdle()

            // Last 7 days drops the pdf (modified 90 days ago).
            vm.selectDate(DateBucket.Week)
            advanceUntilIdle()

            assertThat(
                vm.uiState.value.results
                    .map { it.name }
            ).containsExactly("report.jpg", "report.mp3", "report.zip")
        }

    @Test
    fun `removing a chip restores the broader result set`() =
        runTest(dispatcher) {
            val (vm, _) = viewModel()
            vm.onQueryChange("report")
            advanceUntilIdle()
            vm.toggleType(FileTypeFilter.Image)
            advanceUntilIdle()
            assertThat(vm.uiState.value.results).hasSize(1)

            val chip = vm.uiState.value.activeChips
                .single()
            vm.removeChip(chip)
            advanceUntilIdle()

            assertThat(vm.uiState.value.results).hasSize(4)
            assertThat(vm.uiState.value.activeChips).isEmpty()
        }

    @Test
    fun `active chips reflect every selected dimension`() =
        runTest(dispatcher) {
            val (vm, _) = viewModel()
            vm.onQueryChange("report")
            advanceUntilIdle()

            vm.toggleType(FileTypeFilter.Image)
            vm.selectSize(SizeBucket.Medium)
            vm.selectDate(DateBucket.Today)
            advanceUntilIdle()

            val chips = vm.uiState.value.activeChips
            assertThat(chips).hasSize(3)
            assertThat(chips[0]).isInstanceOf(ActiveFilterChip.Type::class.java)
            assertThat(chips[1]).isInstanceOf(ActiveFilterChip.Size::class.java)
            assertThat(chips[2]).isInstanceOf(ActiveFilterChip.Date::class.java)
        }

    @Test
    fun `selecting the active size bucket again clears it`() =
        runTest(dispatcher) {
            val (vm, _) = viewModel()
            vm.onQueryChange("report")
            advanceUntilIdle()

            vm.selectSize(SizeBucket.Large)
            advanceUntilIdle()
            assertThat(
                vm.uiState.value.results
                    .map { it.name }
            ).containsExactly("report.zip")

            vm.selectSize(SizeBucket.Large)
            advanceUntilIdle()
            assertThat(vm.uiState.value.selectedSize).isNull()
            assertThat(vm.uiState.value.results).hasSize(4)
        }

    @Test
    fun `empty state shows when a filter excludes every match`() =
        runTest(dispatcher) {
            val (vm, _) = viewModel()
            vm.onQueryChange("report")
            advanceUntilIdle()

            vm.toggleType(FileTypeFilter.Video)
            advanceUntilIdle()

            assertThat(vm.uiState.value.results).isEmpty()
            assertThat(vm.uiState.value.isEmpty).isTrue()
        }
}
