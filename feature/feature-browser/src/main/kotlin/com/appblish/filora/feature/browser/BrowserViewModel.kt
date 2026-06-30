package com.appblish.filora.feature.browser

import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appblish.filora.core.common.result.OperationError
import com.appblish.filora.core.common.result.Result
import com.appblish.filora.core.domain.model.FileItem
import com.appblish.filora.core.domain.model.SortOrder
import com.appblish.filora.core.domain.model.ViewLayout
import com.appblish.filora.core.domain.repository.SettingsRepository
import com.appblish.filora.core.domain.usecase.ListDirectoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the Browser (FR-2.1 listing with loading/empty/error/content; FR-2.3 sort;
 * FR-2.4 show-hidden; FR-2.5 pull-to-refresh).
 *
 * The directory listing ([ListDirectoryUseCase]) is collected per (location, sort): a
 * sort change cancels the in-flight listing and re-reads with the new order, while
 * toggling show-hidden only re-filters the already-loaded snapshot ([applyVisible]) so
 * it never costs a directory read. Layout, sort, and show-hidden persist through
 * [SettingsRepository] (T038/T040/T042) so a write re-emits preferences and this VM
 * reacts to its own change, keeping the toolbar and store in lockstep.
 */
@HiltViewModel
class BrowserViewModel
    @Inject
    constructor(
        private val listDirectory: ListDirectoryUseCase,
        private val settingsRepository: SettingsRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(BrowserUiState())
        val uiState: StateFlow<BrowserUiState> = _uiState.asStateFlow()

        private var location: String? = null
        private var rawEntries: List<FileItem> = emptyList()
        private var hasLoaded = false
        private var listJob: Job? = null
        private var preferencesStarted = false

        /** Binds the directory to browse; a blank location resolves to primary storage. */
        fun bindLocation(location: String) {
            val resolved = resolveLocation(location)
            if (this.location == resolved) return
            this.location = resolved
            hasLoaded = false
            rawEntries = emptyList()
            _uiState.update { it.copy(location = resolved, phase = BrowserUiState.Phase.Loading) }
            startPreferences()
            reload()
        }

        fun refresh() {
            _uiState.update { it.copy(isRefreshing = true) }
            reload()
        }

        /** Flips list ⇄ grid and persists it (T038). */
        fun toggleLayout() {
            val next = if (_uiState.value.layout == ViewLayout.List) ViewLayout.Grid else ViewLayout.List
            viewModelScope.launch { settingsRepository.setDefaultViewLayout(next) }
        }

        /**
         * Sorts by [by] (T040): selecting the active key flips its direction, a new key
         * starts ascending. Persisted, which re-reads the directory in the new order.
         */
        fun setSortBy(by: SortOrder.By) {
            val current = _uiState.value.sortOrder
            val next =
                if (current.by == by) {
                    current.copy(ascending = !current.ascending)
                } else {
                    current.copy(by = by, ascending = true)
                }
            viewModelScope.launch { settingsRepository.setDefaultSortOrder(next) }
        }

        /** Persists the show-hidden toggle (T042); re-filters without re-reading. */
        fun setShowHidden(show: Boolean) {
            viewModelScope.launch { settingsRepository.setShowHiddenFiles(show) }
        }

        private fun startPreferences() {
            if (preferencesStarted) return
            preferencesStarted = true
            viewModelScope.launch {
                settingsRepository.preferences.collect { prefs ->
                    val sortChanged = _uiState.value.sortOrder != prefs.defaultSortOrder
                    _uiState.update {
                        it.copy(
                            layout = prefs.defaultViewLayout,
                            sortOrder = prefs.defaultSortOrder,
                            showHidden = prefs.showHiddenFiles,
                        )
                    }
                    if (sortChanged) reload() else applyVisible()
                }
            }
        }

        private fun reload() {
            val loc = location ?: return
            listJob?.cancel()
            listJob =
                viewModelScope.launch {
                    listDirectory(loc, _uiState.value.sortOrder).collect { result ->
                        when (result) {
                            is Result.Success -> {
                                rawEntries = result.data
                                hasLoaded = true
                                applyVisible()
                            }

                            is Result.Error ->
                                _uiState.update {
                                    it.copy(
                                        phase = BrowserUiState.Phase.Error,
                                        isRefreshing = false,
                                        errorMessageRes = result.error.toMessageRes(),
                                    )
                                }
                        }
                    }
                }
        }

        private fun applyVisible() {
            val state = _uiState.value
            val visible = if (state.showHidden) rawEntries else rawEntries.filterNot(FileItem::isHidden)
            _uiState.update {
                it.copy(
                    entries = visible,
                    phase =
                        when {
                            !hasLoaded -> it.phase
                            visible.isEmpty() -> BrowserUiState.Phase.Empty
                            else -> BrowserUiState.Phase.Content
                        },
                    isRefreshing = if (hasLoaded) false else it.isRefreshing,
                    errorMessageRes = if (hasLoaded) null else it.errorMessageRes,
                )
            }
        }

        private fun resolveLocation(location: String): String =
            location.ifBlank { Environment.getExternalStorageDirectory()?.absolutePath ?: DEFAULT_ROOT }

        private companion object {
            const val DEFAULT_ROOT = "/storage/emulated/0"
        }
    }

private fun OperationError.toMessageRes(): Int =
    when (this) {
        is OperationError.PermissionDenied -> R.string.browser_error_permission
        is OperationError.NotFound -> R.string.browser_error_not_found
        else -> R.string.browser_error_generic
    }
