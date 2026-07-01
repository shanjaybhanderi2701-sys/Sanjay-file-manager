package com.appblish.filora.core.domain.model

/**
 * An item currently sitting in the recycle bin (FR-3.4, M12). Decoupled from the
 * persistence [id]-token and the on-disk payload: the UI lists these, and restore /
 * permanent-delete address them by [id].
 *
 * [originalPath] is where [restore][com.appblish.filora.core.domain.usecase.RestoreFromTrashUseCase]
 * puts the item back; [sizeBytes] was captured at delete-time so the bin can show its
 * footprint without re-walking disk; [deletedAtEpochMillis] drives the retention
 * countdown surfaced to the user and the auto-purge policy.
 */
data class TrashedItem(
    val id: String,
    val originalPath: String,
    val name: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val deletedAtEpochMillis: Long,
)
