package com.appblish.filora.core.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.appblish.filora.core.domain.model.SortOrder
import com.appblish.filora.core.domain.model.ThemeMode
import com.appblish.filora.core.domain.model.UserPreferences
import com.appblish.filora.core.domain.model.ViewLayout
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryImplTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testScope = TestScope(UnconfinedTestDispatcher())

    private fun newStore(fileName: String): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            scope = testScope.backgroundScope,
            produceFile = { File(tempFolder.root, fileName) },
        )

    private fun newRepository(fileName: String) = SettingsRepositoryImpl(newStore(fileName))

    @Test
    fun `emits defaults when nothing is persisted`() =
        testScope.runTest {
            val repository = newRepository("empty.preferences_pb")

            assertThat(repository.preferences.first()).isEqualTo(UserPreferences.Default)
        }

    @Test
    fun `persists theme mode`() =
        testScope.runTest {
            val repository = newRepository("theme.preferences_pb")

            repository.setThemeMode(ThemeMode.Dark)

            assertThat(repository.preferences.first().themeMode).isEqualTo(ThemeMode.Dark)
        }

    @Test
    fun `persists dynamic color and hidden files toggles`() =
        testScope.runTest {
            val repository = newRepository("toggles.preferences_pb")

            repository.setUseDynamicColor(false)
            repository.setShowHiddenFiles(true)

            val prefs = repository.preferences.first()
            assertThat(prefs.useDynamicColor).isFalse()
            assertThat(prefs.showHiddenFiles).isTrue()
        }

    @Test
    fun `persists default view layout`() =
        testScope.runTest {
            val repository = newRepository("layout.preferences_pb")

            repository.setDefaultViewLayout(ViewLayout.Grid)

            assertThat(repository.preferences.first().defaultViewLayout).isEqualTo(ViewLayout.Grid)
        }

    @Test
    fun `persists every field of the default sort order`() =
        testScope.runTest {
            val repository = newRepository("sort.preferences_pb")
            val sortOrder =
                SortOrder(by = SortOrder.By.Size, ascending = false, foldersFirst = false)

            repository.setDefaultSortOrder(sortOrder)

            assertThat(repository.preferences.first().defaultSortOrder).isEqualTo(sortOrder)
        }

    @Test
    fun `independent writes do not clobber each other`() =
        testScope.runTest {
            val repository = newRepository("combined.preferences_pb")

            repository.setThemeMode(ThemeMode.Light)
            repository.setShowHiddenFiles(true)
            repository.setDefaultViewLayout(ViewLayout.Grid)

            val prefs = repository.preferences.first()
            assertThat(prefs.themeMode).isEqualTo(ThemeMode.Light)
            assertThat(prefs.showHiddenFiles).isTrue()
            assertThat(prefs.defaultViewLayout).isEqualTo(ViewLayout.Grid)
            // Untouched fields keep their defaults.
            assertThat(prefs.useDynamicColor).isEqualTo(UserPreferences.Default.useDynamicColor)
        }

    @Test
    fun `falls back to default for an unknown stored enum value`() =
        testScope.runTest {
            val store = newStore("corrupt-enum.preferences_pb")
            // Simulate a value written by a future build whose enum constant we no
            // longer recognise; the read must degrade rather than throw.
            store.edit { it[stringPreferencesKey("theme_mode")] = "Solarized" }
            val repository = SettingsRepositoryImpl(store)

            assertThat(repository.preferences.first().themeMode)
                .isEqualTo(UserPreferences.Default.themeMode)
        }
}
