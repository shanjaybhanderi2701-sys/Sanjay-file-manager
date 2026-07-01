package com.appblish.filora.feature.browser.operations

import androidx.annotation.StringRes
import com.appblish.filora.core.domain.model.FileItem

/**
 * The three batch operations that need a destination before they can run (FR-3.2/3.3,
 * FR-7.1). Rename/delete/share are handled inline by the batch bar; these are routed
 * through the [OperationFlowState] state machine because they must first pick where the
 * result goes and (for copy/move) how to resolve name collisions.
 */
enum class BatchOperationKind {
    COPY,
    MOVE,
    ZIP,
}

/**
 * A batch operation the user has started from the action bar, carrying an immutable
 * snapshot of the selected [sources] so the selection can be cleared while the picker
 * and conflict prompt are shown.
 */
data class PendingBatchOperation(
    val kind: BatchOperationKind,
    val sources: List<FileItem>,
) {
    val sourcePaths: List<String> get() = sources.map(FileItem::path)
}

/**
 * The in-app local folder chooser (FR-3.2) used to pick a destination directory for
 * `file://` locations, and always for ZIP (whose worker writes a `java.io.File`).
 * [directories] are the sub-folders of [currentPath]; navigation walks the tree by
 * swapping [currentPath] and re-listing.
 */
data class FolderPickerState(
    val rootPath: String,
    val currentPath: String,
    val directories: List<FileItem> = emptyList(),
    val isLoading: Boolean = true,
    @StringRes val errorMessageRes: Int? = null,
) {
    /** True unless we are already at the picker's root, so "up" never escapes it. */
    val canGoUp: Boolean get() = currentPath != rootPath

    val parentPath: String get() = currentPath.substringBeforeLast('/', rootPath).ifEmpty { rootPath }
}

/**
 * Where a copy/move/zip is in its "choose a destination, then run" flow. Exactly one
 * stage is active at a time; the ViewModel advances it and the screen renders the
 * matching picker/prompt. Cleared to `null` once the operation is enqueued or cancelled.
 */
sealed interface OperationFlowState {
    val request: PendingBatchOperation

    /** Browsing local folders to choose a destination directory (`file://` + all ZIP). */
    data class PickingLocalDestination(
        override val request: PendingBatchOperation,
        val picker: FolderPickerState,
    ) : OperationFlowState

    /**
     * Waiting for the system `ACTION_OPEN_DOCUMENT_TREE` result (copy/move whose current
     * location is a SAF tree). The screen owns the launcher; this stage just tells it to fire.
     */
    data class PickingSafDestination(
        override val request: PendingBatchOperation,
    ) : OperationFlowState

    /** Copy/move destination chosen; picking Skip / Replace / Keep-both (FR-3.3). */
    data class ChoosingConflict(
        override val request: PendingBatchOperation,
        val destinationDir: String,
    ) : OperationFlowState
}

/**
 * A running (or just-enqueued) background operation, rendered as a determinate progress
 * sheet with a cancel control (FR-3.5). [fraction] is `null` while indeterminate
 * (enqueued but not started, or a running item of unknown size); [currentName] is the
 * item being processed. Terminal states are not held here — they clear the sheet and
 * surface a one-shot snackbar instead.
 */
data class ActiveOperation(
    val operationId: String,
    val kind: BatchOperationKind,
    val fraction: Float?,
    val currentName: String?,
)
