package com.appblish.filora.core.domain.repository

import com.appblish.filora.core.domain.model.SortOrder
import com.appblish.filora.core.domain.model.ThemeMode
import com.appblish.filora.core.domain.model.UserPreferences
import com.appblish.filora.core.domain.model.ViewLayout
import kotlinx.coroutines.flow.Flow

/**
 * App preferences (FR-11.1, FR-11.2), DataStore-backed in `core-data`.
 *
 * Reads come through a single [preferences] flow that always emits a complete
 * [UserPreferences] snapshot — defaults on a fresh install, and the last persisted
 * value thereafter — so observers never see a partial state. Writes are per-field
 * suspend functions; each persists atomically and re-emits through [preferences].
 */
interface SettingsRepository {
    /** The current preferences; emits again on every write. */
    val preferences: Flow<UserPreferences>

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun setUseDynamicColor(enabled: Boolean)

    suspend fun setShowHiddenFiles(enabled: Boolean)

    suspend fun setDefaultViewLayout(layout: ViewLayout)

    suspend fun setDefaultSortOrder(sortOrder: SortOrder)
}
