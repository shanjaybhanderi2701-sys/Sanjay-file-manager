package com.appblish.filora.core.domain.model

/**
 * Result of a successful delete (FR-3.4). Reports how many items were removed and
 * whether they were moved to a recoverable trash or deleted permanently, so the UI
 * can offer Undo only when recovery is actually possible (NFR-2.4). A delete is
 * atomic: a [com.appblish.filora.core.common.result.Result.Success] means every
 * requested path was removed; on any failure the data layer leaves all sources
 * intact (NFR-2.2) and returns an error instead.
 */
data class DeleteOutcome(
    val deletedCount: Int,
    val movedToTrash: Boolean,
) {
    /** True when the deleted items can be restored (platform trash was used). */
    val recoverable: Boolean get() = movedToTrash
}
