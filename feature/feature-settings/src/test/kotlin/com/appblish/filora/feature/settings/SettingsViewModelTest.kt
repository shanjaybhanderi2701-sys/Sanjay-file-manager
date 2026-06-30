package com.appblish.filora.feature.settings

import app.cash.turbine.test
import com.appblish.filora.core.domain.model.SortOrder
import com.appblish.filora.core.domain.model.ThemeMode
import com.appblish.filora.core.domain.model.UserPreferences
import com.appblish.filora.core.domain.model.ViewLayout
import com.appblish.filora.core.domain.repository.SettingsRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [SettingsViewModel]. They pin the two behaviours the screen
 * relies on: [uiState] mirrors whatever the repository emits, and every control
 * delegates straight to the repository so DataStore stays the single source of
 * truth (no local mutable copy that could drift).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeSettingsRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState reflects the repository preferences`() =
        runTest {
            repository.emit(UserPreferences(themeMode = ThemeMode.Dark, showHiddenFiles = true))
            val viewModel = SettingsViewModel(repository)

            viewModel.uiState.test {
                // Seed value before the flow is collected.
                assertThat(awaitItem().isLoading).isTrue()
                val loaded = awaitItem()
                assertThat(loaded.isLoading).isFalse()
                assertThat(loaded.preferences.themeMode).isEqualTo(ThemeMode.Dark)
                assertThat(loaded.preferences.showHiddenFiles).isTrue()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `setters delegate to the repository`() =
        runTest {
            val viewModel = SettingsViewModel(repository)

            viewModel.setThemeMode(ThemeMode.Light)
            viewModel.setUseDynamicColor(false)
            viewModel.setShowHiddenFiles(true)
            viewModel.setDefaultViewLayout(ViewLayout.Grid)
            viewModel.setDefaultSortOrder(SortOrder(by = SortOrder.By.Size, ascending = false))
            advanceUntilIdle()

            val prefs = repository.current()
            assertThat(prefs.themeMode).isEqualTo(ThemeMode.Light)
            assertThat(prefs.useDynamicColor).isFalse()
            assertThat(prefs.showHiddenFiles).isTrue()
            assertThat(prefs.defaultViewLayout).isEqualTo(ViewLayout.Grid)
            assertThat(prefs.defaultSortOrder.by).isEqualTo(SortOrder.By.Size)
            assertThat(prefs.defaultSortOrder.ascending).isFalse()
        }
}

/** In-memory [SettingsRepository] for ViewModel tests. */
private class FakeSettingsRepository : SettingsRepository {
    private val state = MutableStateFlow(UserPreferences.Default)

    override val preferences: Flow<UserPreferences> = state

    fun emit(value: UserPreferences) = state.update { value }

    fun current(): UserPreferences = state.value

    override suspend fun setThemeMode(mode: ThemeMode) = state.update { it.copy(themeMode = mode) }

    override suspend fun setUseDynamicColor(enabled: Boolean) = state.update { it.copy(useDynamicColor = enabled) }

    override suspend fun setShowHiddenFiles(enabled: Boolean) = state.update { it.copy(showHiddenFiles = enabled) }

    override suspend fun setDefaultViewLayout(layout: ViewLayout) = state.update { it.copy(defaultViewLayout = layout) }

    override suspend fun setDefaultSortOrder(sortOrder: SortOrder) =
        state.update { it.copy(defaultSortOrder = sortOrder) }
}
