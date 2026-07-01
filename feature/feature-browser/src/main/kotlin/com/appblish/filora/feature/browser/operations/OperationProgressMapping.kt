package com.appblish.filora.feature.browser.operations

import androidx.annotation.StringRes
import com.appblish.filora.core.common.result.OperationError
import com.appblish.filora.core.domain.model.ArchiveProgress
import com.appblish.filora.core.domain.model.FileItem
import com.appblish.filora.core.domain.model.OperationProgress
import com.appblish.filora.feature.browser.R

/**
 * A single mapped step of a background operation the browser is observing. [Running]
 * refreshes the progress sheet; [Terminal] clears it and carries the one-shot snackbar
 * message plus whether the operation [succeeded] (so the ViewModel can reload on success).
 * Keeping this mapping pure lets the ViewModel logic be exercised without Android/WorkManager.
 */
sealed interface OperationUpdate {
    data class Running(val active: ActiveOperation) : OperationUpdate

    data class Terminal(
        @StringRes val messageRes: Int,
        val succeeded: Boolean,
    ) : OperationUpdate
}

/** Maps a copy/move progress emission for [operationId]/[kind] to an [OperationUpdate]. */
fun OperationProgress.toUpdate(
    operationId: String,
    kind: BatchOperationKind,
): OperationUpdate =
    when (this) {
        is OperationProgress.Pending ->
            OperationUpdate.Running(ActiveOperation(operationId, kind, fraction = null, currentName = null))

        is OperationProgress.Running ->
            OperationUpdate.Running(ActiveOperation(operationId, kind, fraction = fraction, currentName = currentName))

        is OperationProgress.Succeeded ->
            OperationUpdate.Terminal(successMessageRes(kind), succeeded = true)

        is OperationProgress.Failed ->
            OperationUpdate.Terminal(error.toMessageRes(), succeeded = false)

        is OperationProgress.Cancelled ->
            OperationUpdate.Terminal(R.string.browser_op_cancelled, succeeded = false)
    }

/** Maps a ZIP compression progress emission for [operationId] to an [OperationUpdate]. */
fun ArchiveProgress.toUpdate(operationId: String): OperationUpdate =
    when (this) {
        is ArchiveProgress.Pending ->
            OperationUpdate.Running(
                ActiveOperation(operationId, BatchOperationKind.ZIP, fraction = null, currentName = null),
            )

        is ArchiveProgress.Running ->
            OperationUpdate.Running(
                ActiveOperation(operationId, BatchOperationKind.ZIP, fraction = fraction, currentName = currentName),
            )

        is ArchiveProgress.Succeeded ->
            OperationUpdate.Terminal(R.string.browser_zipped, succeeded = true)

        is ArchiveProgress.Failed ->
            OperationUpdate.Terminal(error.toMessageRes(), succeeded = false)

        is ArchiveProgress.Cancelled ->
            OperationUpdate.Terminal(R.string.browser_op_cancelled, succeeded = false)
    }

/**
 * The full path of the archive to create when zipping [sources] into [destinationDir]:
 * the single source's base name (or "archive" for multi-select), suffixed `.zip`.
 */
fun archiveDestinationPath(
    destinationDir: String,
    sources: List<FileItem>,
): String {
    val base =
        sources.singleOrNull()?.name?.substringBeforeLast('.')?.takeIf { it.isNotBlank() } ?: DEFAULT_ARCHIVE_NAME
    return destinationDir.trimEnd('/') + "/" + base + ".zip"
}

@StringRes
private fun successMessageRes(kind: BatchOperationKind): Int =
    when (kind) {
        BatchOperationKind.COPY -> R.string.browser_copied
        BatchOperationKind.MOVE -> R.string.browser_moved
        BatchOperationKind.ZIP -> R.string.browser_zipped
    }

@StringRes
private fun OperationError.toMessageRes(): Int =
    when (this) {
        is OperationError.PermissionDenied -> R.string.browser_error_permission
        is OperationError.NotFound -> R.string.browser_error_not_found
        else -> R.string.browser_error_generic
    }

private const val DEFAULT_ARCHIVE_NAME = "archive"
