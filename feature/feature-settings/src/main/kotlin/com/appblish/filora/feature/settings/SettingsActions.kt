package com.appblish.filora.feature.settings

import com.appblish.filora.core.domain.model.SortOrder
import com.appblish.filora.core.domain.model.ThemeMode
import com.appblish.filora.core.domain.model.ViewLayout

/** Callbacks the settings controls invoke; one per persisted preference. */
data class SettingsActions(
    val onThemeMode: (ThemeMode) -> Unit = {},
    val onDynamicColor: (Boolean) -> Unit = {},
    val onShowHidden: (Boolean) -> Unit = {},
    val onViewLayout: (ViewLayout) -> Unit = {},
    val onSortBy: (SortOrder.By) -> Unit = {},
    val onSortAscending: (Boolean) -> Unit = {},
    val onFoldersFirst: (Boolean) -> Unit = {},
)
