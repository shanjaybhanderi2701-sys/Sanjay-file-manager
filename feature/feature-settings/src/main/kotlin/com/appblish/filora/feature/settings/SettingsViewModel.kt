package com.appblish.filora.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appblish.filora.core.domain.model.SortOrder
import com.appblish.filora.core.domain.model.ThemeMode
import com.appblish.filora.core.domain.model.UserPreferences
import com.appblish.filora.core.domain.model.ViewLayout
import com.appblish.filora.core.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Immutable snapshot the settings screen renders (FR-11.1, FR-11.2). */
data class SettingsUiState(
    val preferences: UserPreferences = UserPreferences.Default,
    val isLoading: Boolean = true,
)

/**
 * Settings screen ViewModel (M7 T7.1). Mirrors the DataStore-backed
 * [SettingsRepository.preferences] into [uiState] and forwards each toggle straight
 * back to the repository — there is no local mutable copy, so the persisted store
 * is the single source of truth and the UI reflects exactly what survives a
 * restart. Writes are fire-and-forget on [viewModelScope]; the resulting value
 * re-emits through the same flow, so the control updates only once persistence
 * succeeds.
 */
@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val settingsRepository: SettingsRepository,
    ) : ViewModel() {
        val uiState: StateFlow<SettingsUiState> =
            settingsRepository.preferences
                .map { SettingsUiState(preferences = it, isLoading = false) }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                    initialValue = SettingsUiState(),
                )

        fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { settingsRepository.setThemeMode(mode) }

        fun setUseDynamicColor(enabled: Boolean) =
            viewModelScope.launch { settingsRepository.setUseDynamicColor(enabled) }

        fun setShowHiddenFiles(enabled: Boolean) =
            viewModelScope.launch { settingsRepository.setShowHiddenFiles(enabled) }

        fun setDefaultViewLayout(layout: ViewLayout) =
            viewModelScope.launch { settingsRepository.setDefaultViewLayout(layout) }

        fun setDefaultSortOrder(sortOrder: SortOrder) =
            viewModelScope.launch { settingsRepository.setDefaultSortOrder(sortOrder) }

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }
