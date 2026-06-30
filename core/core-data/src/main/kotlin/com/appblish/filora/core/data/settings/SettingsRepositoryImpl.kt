package com.appblish.filora.core.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.appblish.filora.core.domain.model.SortOrder
import com.appblish.filora.core.domain.model.ThemeMode
import com.appblish.filora.core.domain.model.UserPreferences
import com.appblish.filora.core.domain.model.ViewLayout
import com.appblish.filora.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

/**
 * [SettingsRepository] over a Preferences [DataStore] (FR-11.1, FR-11.2).
 *
 * The aggregate [UserPreferences] is decomposed into one primitive key per field
 * (enums stored by `name`, [SortOrder] flattened into its three components) so the
 * store stays a plain key/value file with no schema migration. Reads fall back to
 * [UserPreferences.Default] for missing or unparseable values, so a partially
 * written store — or an enum constant removed in a future version — degrades to a
 * sane default instead of throwing. A transient [IOException] while reading the
 * file is swallowed to an empty snapshot for the same reason.
 */
class SettingsRepositoryImpl
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) : SettingsRepository {
        override val preferences: Flow<UserPreferences> =
            dataStore.data
                .catch { error ->
                    if (error is IOException) emit(emptyPreferences()) else throw error
                }.map { it.toUserPreferences() }

        override suspend fun setThemeMode(mode: ThemeMode) {
            dataStore.edit { it[Keys.THEME_MODE] = mode.name }
        }

        override suspend fun setUseDynamicColor(enabled: Boolean) {
            dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
        }

        override suspend fun setShowHiddenFiles(enabled: Boolean) {
            dataStore.edit { it[Keys.SHOW_HIDDEN] = enabled }
        }

        override suspend fun setDefaultViewLayout(layout: ViewLayout) {
            dataStore.edit { it[Keys.VIEW_LAYOUT] = layout.name }
        }

        override suspend fun setDefaultSortOrder(sortOrder: SortOrder) {
            dataStore.edit { prefs ->
                prefs[Keys.SORT_BY] = sortOrder.by.name
                prefs[Keys.SORT_ASCENDING] = sortOrder.ascending
                prefs[Keys.SORT_FOLDERS_FIRST] = sortOrder.foldersFirst
            }
        }

        private fun Preferences.toUserPreferences(): UserPreferences {
            val defaults = UserPreferences.Default
            return UserPreferences(
                themeMode = enumOrDefault(this[Keys.THEME_MODE], defaults.themeMode),
                useDynamicColor = this[Keys.DYNAMIC_COLOR] ?: defaults.useDynamicColor,
                showHiddenFiles = this[Keys.SHOW_HIDDEN] ?: defaults.showHiddenFiles,
                defaultViewLayout = enumOrDefault(this[Keys.VIEW_LAYOUT], defaults.defaultViewLayout),
                defaultSortOrder =
                    SortOrder(
                        by = enumOrDefault(this[Keys.SORT_BY], defaults.defaultSortOrder.by),
                        ascending = this[Keys.SORT_ASCENDING] ?: defaults.defaultSortOrder.ascending,
                        foldersFirst = this[Keys.SORT_FOLDERS_FIRST] ?: defaults.defaultSortOrder.foldersFirst,
                    ),
            )
        }

        /** Resolves a stored enum `name`, falling back to [fallback] for null/unknown values. */
        private inline fun <reified E : Enum<E>> enumOrDefault(
            stored: String?,
            fallback: E
        ): E = stored?.let { name -> enumValues<E>().firstOrNull { it.name == name } } ?: fallback

        private object Keys {
            val THEME_MODE = stringPreferencesKey("theme_mode")
            val DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
            val SHOW_HIDDEN = booleanPreferencesKey("show_hidden_files")
            val VIEW_LAYOUT = stringPreferencesKey("default_view_layout")
            val SORT_BY = stringPreferencesKey("sort_by")
            val SORT_ASCENDING = booleanPreferencesKey("sort_ascending")
            val SORT_FOLDERS_FIRST = booleanPreferencesKey("sort_folders_first")
        }
    }
