package com.appblish.filora.feature.browser

import android.os.Environment
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appblish.filora.core.common.result.OperationError
import com.appblish.filora.core.common.result.Result
import com.appblish.filora.core.domain.model.FileItem
import com.appblish.filora.core.domain.model.SortOrder
import com.appblish.filora.core.domain.model.ViewLayout
import com.appblish.filora.core.domain.repository.SettingsRepository
import com.appblish.filora.core.domain.usecase.CreateFolderUseCase
import com.appblish.filora.core.domain.usecase.DeleteUseCase
import com.appblish.filora.core.domain.usecase.ListDirectoryUseCase
import com.appblish.filora.core.domain.usecase.ObserveFavoritesUseCase
import com.appblish.filora.core.domain.usecase.RenameUseCase
import com.appblish.filora.core.domain.usecase.ToggleFavoriteUseCase
import com.appblish.filora.feature.browser.selection.MultiSelectReducer
import com.appblish.filora.feature.browser.selection.SelectionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the Browser (FR-2.1 listing with loading/empty/error/content; FR-2.3 sort;
 * FR-2.4 show-hidden; FR-2.5 pull-to-refresh) and its file operations (FR-3.1/3.2/3.4
 * create/rename/delete with FR-4.1 multi-select).
 *
 * The directory listing ([ListDirectoryUseCase]) is collected per (location, sort): a
 * sort change cancels the in-flight listing and re-reads with the new order, while
 * toggling show-hidden only re-filters the already-loaded snapshot ([applyVisible]) so
 * it never costs a directory read. Layout, sort, and show-hidden persist through
 * [SettingsRepository] (T038/T040/T042) so a write re-emits preferences and this VM
 * reacts to its own change, keeping the toolbar and store in lockstep.
 *
 * Favorites (FR-9.1, T094): the pinned-path set is observed from the Room-backed
 * [ObserveFavoritesUseCase] and exposed on the state so each row knows whether to
 * offer "pin" or "unpin" in its context menu. [toggleFavorite] flips the pin via
 * [ToggleFavoriteUseCase]; the observed stream re-emits and refreshes the affordance.
 *
 * Create/rename/delete mutate [rawEntries] in place and re-derive the visible list
 * (T077 — no full reload), so the row appears/updates/disappears immediately while the
 * underlying listing Flow reconciles in the background.
 */
@HiltViewModel
class BrowserViewModel
    @Inject
    constructor(
        private val listDirectory: ListDirectoryUseCase,
        private val settingsRepository: SettingsRepository,
        private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
        observeFavorites: ObserveFavoritesUseCase,
        private val createFolderUseCase: CreateFolderUseCase,
        private val renameUseCase: RenameUseCase,
        private val deleteUseCase: DeleteUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(BrowserUiState())
        val uiState: StateFlow<BrowserUiState> = _uiState.asStateFlow()

        private var location: String? = null
        private var rawEntries: List<FileItem> = emptyList()
        private var hasLoaded = false
        private var listJob: Job? = null
        private var preferencesStarted = false

        init {
            observeFavorites()
                .onEach { favorites ->
                    val paths = favorites.mapTo(mutableSetOf(), FileItem::path)
                    _uiState.update { it.copy(favoritePaths = paths) }
                }.launchIn(viewModelScope)
        }

        /** Binds the directory to browse; a blank location resolves to primary storage. */
        fun bindLocation(location: String) {
            val resolved = resolveLocation(location)
            if (this.location == resolved) return
            this.location = resolved
            hasLoaded = false
            rawEntries = emptyList()
            _uiState.update {
                it.copy(
                    location = resolved,
                    phase = BrowserUiState.Phase.Loading,
                    selection = SelectionState(),
                    dialog = null,
                )
            }
            startPreferences()
            reload()
        }

        /** Pins or unpins [item] (FR-9.1); the observed favorites stream updates the UI. */
        fun toggleFavorite(item: FileItem) {
            viewModelScope.launch { toggleFavoriteUseCase(item) }
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

        // ---- Multi-select (T075) -------------------------------------------------

        /** Long-press, or an in-mode tap: toggles [item]'s membership in the selection. */
        fun toggleSelection(item: FileItem) {
            _uiState.update {
                it.copy(selection = MultiSelectReducer.toggle(it.selection, item.path, item.isDirectory))
            }
        }

        /** Selects every currently visible entry (select-all from the action bar). */
        fun selectAll() {
            _uiState.update { state ->
                state.copy(
                    selection =
                        MultiSelectReducer.selectAll(
                            state.selection,
                            state.entries.associate { it.path to it.isDirectory },
                        ),
                )
            }
        }

        /** Clears the selection and exits selection mode (Back or the bar's close). */
        fun clearSelection() {
            _uiState.update { it.copy(selection = SelectionState()) }
        }

        // ---- Dialogs (T066–T068) -------------------------------------------------

        fun showNewFolderDialog() = _uiState.update { it.copy(dialog = BrowserDialog.NewFolder) }

        fun showRenameDialog(item: FileItem) =
            _uiState.update { it.copy(dialog = BrowserDialog.Rename(item.path, item.name)) }

        fun showDeleteDialog() = _uiState.update { it.copy(dialog = BrowserDialog.ConfirmDelete) }

        fun dismissDialog() = _uiState.update { it.copy(dialog = null) }

        /** Acknowledges the one-shot snackbar message once the screen has shown it. */
        fun clearMessage() = _uiState.update { it.copy(messageRes = null) }

        /** Surfaces a one-shot snackbar from the screen (e.g. a share with no handler). */
        fun showMessage(
            @StringRes messageRes: Int,
        ) = _uiState.update { it.copy(messageRes = messageRes) }

        // ---- Operations (FR-3.1/3.2/3.4) ----------------------------------------

        /** Creates a folder in the current directory and shows it in place (T066/T077). */
        fun confirmCreateFolder(rawName: String) {
            val parent = location ?: return
            viewModelScope.launch {
                when (val result = createFolderUseCase(parent, rawName)) {
                    is Result.Success -> {
                        rawEntries = rawEntries + result.data
                        _uiState.update { it.copy(dialog = null) }
                        applyVisible()
                    }

                    is Result.Error ->
                        _uiState.update { it.copy(dialog = null, messageRes = result.error.toMessageRes()) }
                }
            }
        }

        /** Renames the dialog's target and swaps it in place without a reload (T067/T077). */
        fun confirmRename(rawName: String) {
            val target = _uiState.value.dialog as? BrowserDialog.Rename ?: return
            viewModelScope.launch {
                when (val result = renameUseCase(target.path, rawName, target.currentName)) {
                    is Result.Success -> {
                        rawEntries = rawEntries.map { if (it.path == target.path) result.data else it }
                        _uiState.update { it.copy(dialog = null, selection = SelectionState()) }
                        applyVisible()
                    }

                    is Result.Error ->
                        _uiState.update { it.copy(dialog = null, messageRes = result.error.toMessageRes()) }
                }
            }
        }

        /** Deletes the current selection and removes the rows in place (T068/T077). */
        fun confirmDelete() {
            val paths = _uiState.value.selection.selected.keys
                .toList()
            if (paths.isEmpty()) {
                _uiState.update { it.copy(dialog = null) }
                return
            }
            viewModelScope.launch {
                when (val result = deleteUseCase(paths, toTrash = true)) {
                    is Result.Success -> {
                        val removed = paths.toSet()
                        rawEntries = rawEntries.filterNot { it.path in removed }
                        _uiState.update {
                            it.copy(
                                dialog = null,
                                selection = SelectionState(),
                                messageRes = R.string.browser_deleted,
                            )
                        }
                        applyVisible()
                    }

                    is Result.Error ->
                        _uiState.update { it.copy(dialog = null, messageRes = result.error.toMessageRes()) }
                }
            }
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
