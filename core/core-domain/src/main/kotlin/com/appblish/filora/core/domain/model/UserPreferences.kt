package com.appblish.filora.core.domain.model

/**
 * The full set of user-configurable app preferences (FR-11.1, FR-11.2), persisted
 * by the DataStore-backed `SettingsRepository`. Exposed as one aggregate so the
 * theme host and settings screen observe a single, always-complete snapshot rather
 * than juggling per-key flows. [Default] is what a fresh install reads before the
 * user changes anything (and the fallback when the store can't be read).
 */
data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.System,
    val useDynamicColor: Boolean = true,
    val showHiddenFiles: Boolean = false,
    val defaultViewLayout: ViewLayout = ViewLayout.List,
    val defaultSortOrder: SortOrder = SortOrder.Default,
) {
    companion object {
        val Default = UserPreferences()
    }
}
