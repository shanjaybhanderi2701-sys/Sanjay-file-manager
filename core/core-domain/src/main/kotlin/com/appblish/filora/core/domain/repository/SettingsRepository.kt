package com.appblish.filora.core.domain.repository

import com.appblish.filora.core.domain.model.SortOrder
import com.appblish.filora.core.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

/** App preferences, DataStore-backed in `core-data`. */
interface SettingsRepository {
    val themeMode: Flow<ThemeMode>
    val useDynamicColor: Flow<Boolean>
    val showHiddenFiles: Flow<Boolean>
    val defaultSortOrder: Flow<SortOrder>

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun setUseDynamicColor(enabled: Boolean)

    suspend fun setShowHiddenFiles(enabled: Boolean)

    suspend fun setDefaultSortOrder(sortOrder: SortOrder)
}
