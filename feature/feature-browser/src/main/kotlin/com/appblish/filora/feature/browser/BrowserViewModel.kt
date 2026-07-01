package com.appblish.filora.feature.browser

import android.os.Environment
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appblish.filora.core.common.result.OperationError
import com.appblish.filora.core.common.result.Result
import com.appblish.filora.core.domain.model.ConflictStrategy
import com.appblish.filora.core.domain.model.FileItem
import com.appblish.filora.core.domain.model.FileOperationKind
import com.appblish.filora.core.domain.model.SortOrder
import com.appblish.filora.core.domain.model.ViewLayout
import com.appblish.filora.core.domain.repository.FileOperationsScheduler
import com.appblish.filora.core.domain.repository.SettingsRepository
import com.appblish.filora.core.domain.usecase.CreateFolderUseCase
import com.appblish.filora.core.domain.usecase.DeleteUseCase
import com.appblish.filora.core.domain.usecase.ListDirectoryUseCase
import com.appblish.filora.core.domain.usecase.ObserveFavoritesUseCase
import com.appblish.filora.core.domain.usecase.RenameUseCase
import com.appblish.filora.core.domain.usecase.ToggleFavoriteUseCase
import com.appblish.filora.feature.browser.operations.ActiveOperation
import com.appblish.filora.feature.browser.operations.BatchOperationKind
import com.appblish.filora.feature.browser.operations.FolderPickerState
import com.appblish.filora.feature.browser.operations.OperationFlowState
import com.appblish.filora.feature.browser.operations.OperationUpdate
import com.appblish.filora.feature.browser.operations.PendingBatchOperation
import com.appblish.filora.feature.browser.operations.archiveDestinationPath
import com.appblish.filora.feature.browser.operations.toUpdate
import com.appblish.filora.feature.browser.selection.MultiSelectReducer
import com.appblish.filora.feature.browser.selection.SelectionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
        private val fileOperationsScheduler: FileOperationsScheduler,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(BrowserUiState())
        val uiState: StateFlow<BrowserUiState> = _uiState.asStateFlow()

        private var location: String? = null
        private var rawEntries: List<FileItem> = emptyList()
        private var hasLoaded = false
        private var listJob: Job? = null
        private var preferencesStarted = false
        private var pickerJob: Job? = null
        private var operationJob: Job? = null

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
            pickerJob?.cancel()
            _uiState.update {
                it.copy(
                    location = resolved,
                    phase = BrowserUiState.Phase.Loading,
                    selection = SelectionState(),
                    dialog = null,
                    operationFlow = null,
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

        // ---- Copy / Move / Zip (T069/T070 destination picker, T079 progress) ----

        /**
         * Starts a copy/move/zip from the batch bar (FR-3.2/3.3, FR-7.1). Snapshots the
         * current selection so it can be cleared while the destination is chosen. Copy/move
         * over a SAF tree defer to the system `ACTION_OPEN_DOCUMENT_TREE` picker; everything
         * else (all `file://` copy/move, and every ZIP — whose worker writes a local file)
         * uses the in-app folder chooser. Ignored while an operation is already in flight.
         */
        fun beginBatchOperation(kind: BatchOperationKind) {
            val currentLocation = location ?: return
            val state = _uiState.value
            if (state.activeOperation != null) return
            val sources = state.selectedItems()
            if (sources.isEmpty()) return
            val request = PendingBatchOperation(kind, sources)

            if (kind != BatchOperationKind.ZIP && isSaf(currentLocation)) {
                _uiState.update { it.copy(operationFlow = OperationFlowState.PickingSafDestination(request)) }
            } else {
                // Root the chooser at primary storage so any local folder is reachable; start at
                // the current directory when it is itself a local path. (DEFAULT_ROOT keeps this
                // free of android.os.Environment so the flow stays unit-testable.)
                val root = DEFAULT_ROOT
                val start = if (!isSaf(currentLocation)) currentLocation else root
                _uiState.update {
                    it.copy(
                        operationFlow =
                            OperationFlowState.PickingLocalDestination(
                                request,
                                FolderPickerState(rootPath = root, currentPath = start),
                            ),
                    )
                }
                loadPickerFolders(start)
            }
        }

        /** Navigates the folder chooser into [directory]. */
        fun pickerEnter(directory: FileItem) {
            val flow = _uiState.value.operationFlow as? OperationFlowState.PickingLocalDestination ?: return
            if (!directory.isDirectory) return
            navigatePicker(flow, directory.path)
        }

        /** Walks the folder chooser up one level, never above its root. */
        fun pickerUp() {
            val flow = _uiState.value.operationFlow as? OperationFlowState.PickingLocalDestination ?: return
            if (!flow.picker.canGoUp) return
            navigatePicker(flow, flow.picker.parentPath)
        }

        private fun navigatePicker(
            flow: OperationFlowState.PickingLocalDestination,
            path: String,
        ) {
            val picker = flow.picker.copy(currentPath = path, isLoading = true, errorMessageRes = null)
            _uiState.update { it.copy(operationFlow = flow.copy(picker = picker)) }
            loadPickerFolders(path)
        }

        /** Confirms the folder chooser's current directory as the destination. */
        fun pickerConfirm() {
            val flow = _uiState.value.operationFlow as? OperationFlowState.PickingLocalDestination ?: return
            proceedWithDestination(flow.request, flow.picker.currentPath)
        }

        /** Receives the SAF tree the system picker returned as the copy/move destination. */
        fun onSafDestinationPicked(destinationTreeUri: String) {
            val flow = _uiState.value.operationFlow as? OperationFlowState.PickingSafDestination ?: return
            proceedWithDestination(flow.request, destinationTreeUri)
        }

        /** Chooses the conflict strategy for a copy/move and enqueues it (FR-3.3). */
        fun chooseConflict(strategy: ConflictStrategy) {
            val flow = _uiState.value.operationFlow as? OperationFlowState.ChoosingConflict ?: return
            enqueueCopyMove(flow.request, flow.destinationDir, strategy)
        }

        /** Dismisses the destination picker / conflict prompt without enqueuing anything. */
        fun cancelOperationFlow() {
            pickerJob?.cancel()
            _uiState.update { it.copy(operationFlow = null) }
        }

        /** Cancels the in-flight operation (T074); its progress stream reports the cancellation. */
        fun cancelActiveOperation() {
            val id = _uiState.value.activeOperation?.operationId ?: return
            fileOperationsScheduler.cancel(id)
        }

        private fun proceedWithDestination(
            request: PendingBatchOperation,
            destinationDir: String,
        ) {
            when (request.kind) {
                BatchOperationKind.ZIP -> enqueueZip(request, destinationDir)
                BatchOperationKind.COPY, BatchOperationKind.MOVE ->
                    _uiState.update {
                        it.copy(operationFlow = OperationFlowState.ChoosingConflict(request, destinationDir))
                    }
            }
        }

        private fun loadPickerFolders(path: String) {
            pickerJob?.cancel()
            pickerJob =
                viewModelScope.launch {
                    when (val result = listDirectory(path).first()) {
                        is Result.Success ->
                            updatePicker(path) {
                                it.copy(
                                    directories = result.data.filter(FileItem::isDirectory),
                                    isLoading = false,
                                    errorMessageRes = null,
                                )
                            }

                        is Result.Error ->
                            updatePicker(path) {
                                it.copy(
                                    directories = emptyList(),
                                    isLoading = false,
                                    errorMessageRes = result.error.toMessageRes(),
                                )
                            }
                    }
                }
        }

        private fun updatePicker(
            path: String,
            transform: (FolderPickerState) -> FolderPickerState,
        ) {
            _uiState.update { state ->
                val flow = state.operationFlow as? OperationFlowState.PickingLocalDestination ?: return@update state
                // Ignore a stale listing that resolved after the user navigated elsewhere.
                if (flow.picker.currentPath != path) return@update state
                state.copy(operationFlow = flow.copy(picker = transform(flow.picker)))
            }
        }

        private fun enqueueCopyMove(
            request: PendingBatchOperation,
            destinationDir: String,
            strategy: ConflictStrategy,
        ) {
            val kind =
                if (request.kind == BatchOperationKind.COPY) FileOperationKind.Copy else FileOperationKind.Move
            val operationId =
                fileOperationsScheduler.enqueue(
                    kind = kind,
                    sources = request.sourcePaths,
                    destinationDir = destinationDir,
                    conflictStrategy = strategy,
                )
            startOperation(operationId, request.kind)
            observe {
                fileOperationsScheduler.progress(operationId).collect { progress ->
                    onOperationUpdate(progress.toUpdate(operationId, request.kind))
                }
            }
        }

        private fun enqueueZip(
            request: PendingBatchOperation,
            destinationDir: String,
        ) {
            val archivePath = archiveDestinationPath(destinationDir, request.sources)
            val operationId = fileOperationsScheduler.enqueueCompress(request.sourcePaths, archivePath)
            startOperation(operationId, BatchOperationKind.ZIP)
            observe {
                fileOperationsScheduler.compressProgress(operationId).collect { progress ->
                    onOperationUpdate(progress.toUpdate(operationId))
                }
            }
        }

        private fun startOperation(
            operationId: String,
            kind: BatchOperationKind,
        ) {
            _uiState.update {
                it.copy(
                    operationFlow = null,
                    selection = SelectionState(),
                    activeOperation = ActiveOperation(operationId, kind, fraction = null, currentName = null),
                )
            }
        }

        private fun observe(block: suspend () -> Unit) {
            operationJob?.cancel()
            operationJob = viewModelScope.launch { block() }
        }

        private fun onOperationUpdate(update: OperationUpdate) {
            when (update) {
                is OperationUpdate.Running -> _uiState.update { it.copy(activeOperation = update.active) }
                is OperationUpdate.Terminal -> {
                    _uiState.update { it.copy(activeOperation = null, messageRes = update.messageRes) }
                    if (update.succeeded) reload()
                }
            }
        }

        private fun isSaf(location: String): Boolean = location.startsWith(CONTENT_SCHEME)

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
            const val CONTENT_SCHEME = "content://"
        }
    }

private fun OperationError.toMessageRes(): Int =
    when (this) {
        is OperationError.PermissionDenied -> R.string.browser_error_permission
        is OperationError.NotFound -> R.string.browser_error_not_found
        else -> R.string.browser_error_generic
    }
