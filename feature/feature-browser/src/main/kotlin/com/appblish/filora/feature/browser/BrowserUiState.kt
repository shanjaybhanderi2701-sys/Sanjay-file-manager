package com.appblish.filora.feature.browser

import androidx.annotation.StringRes
import com.appblish.filora.core.domain.model.FileItem
import com.appblish.filora.core.domain.model.SortOrder
import com.appblish.filora.core.domain.model.ViewLayout
import com.appblish.filora.feature.browser.operations.ActiveOperation
import com.appblish.filora.feature.browser.operations.OperationFlowState
import com.appblish.filora.feature.browser.selection.SelectionState

/**
 * Everything the Browser screen renders (T036, FR-2.1). [entries] is the already
 * ordered-and-hidden-filtered list the UI draws; [phase] is the single source of truth
 * for which of the loading/empty/error/content surfaces shows (T044). Layout, sort, and
 * show-hidden mirror the persisted preferences so the toolbar reflects them without a
 * second read. [favoritePaths] is the set of currently-pinned paths (FR-9.1, T094) so a
 * row's context menu can show "pin" or "unpin".
 *
 * [selection] drives multi-select mode and the batch action bar (T075/T076);
 * [dialog] is the currently open create/rename/delete dialog (T066–T068); and
 * [messageRes] is a one-shot snackbar message (operation result or failure, T079)
 * the screen shows and then clears via [BrowserViewModel.clearMessage].
 *
 * [operationFlow] drives the copy/move/zip destination picker + conflict prompt
 * (T069/T070) and [activeOperation] the live worker-progress sheet (T079). Both are
 * `null` when no long operation is being set up or is in flight.
 */
data class BrowserUiState(
    val location: String = "",
    val phase: Phase = Phase.Loading,
    val entries: List<FileItem> = emptyList(),
    val layout: ViewLayout = ViewLayout.List,
    val sortOrder: SortOrder = SortOrder.Default,
    val showHidden: Boolean = false,
    val isRefreshing: Boolean = false,
    val favoritePaths: Set<String> = emptySet(),
    @StringRes val errorMessageRes: Int? = null,
    val selection: SelectionState = SelectionState(),
    val dialog: BrowserDialog? = null,
    @StringRes val messageRes: Int? = null,
    val operationFlow: OperationFlowState? = null,
    val activeOperation: ActiveOperation? = null,
) {
    enum class Phase { Loading, Content, Empty, Error }

    val isContent: Boolean get() = phase == Phase.Content
    val isEmpty: Boolean get() = phase == Phase.Empty
    val isLoading: Boolean get() = phase == Phase.Loading
    val isError: Boolean get() = phase == Phase.Error

    /** True while a multi-selection is active — taps toggle instead of opening (FR-4.1). */
    val inSelectionMode: Boolean get() = selection.isActive

    /** The currently selected [entries], resolved from the selection's paths. */
    fun selectedItems(): List<FileItem> = entries.filter { selection.isSelected(it.path) }
}

/**
 * The create/rename/delete dialog currently shown over the browser, if any. Modelled
 * as a sealed type so the screen renders exactly one and the ViewModel owns the
 * transitions; [Rename] carries the single target identified when the dialog opened.
 */
sealed interface BrowserDialog {
    data object NewFolder : BrowserDialog

    data class Rename(
        val path: String,
        val currentName: String,
    ) : BrowserDialog

    data object ConfirmDelete : BrowserDialog
}
