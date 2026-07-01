package com.appblish.filora.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A file or folder that was moved to the app-managed recycle bin (FR-3.4, T123).
 *
 * [id] is an opaque token (a UUID) that doubles as the on-disk name of the trashed
 * payload under the trash directory, so two files deleted from different folders that
 * share a name never collide. The metadata needed to list, size, restore and
 * auto-purge the bin lives here; the payload bytes live on disk under `trash/<id>`.
 *
 * [originalPath] is where the item is restored to; [sizeBytes] is captured at
 * delete-time so the bin can show its footprint without re-walking the disk; and
 * [deletedAtEpochMillis] drives the retention/auto-purge policy (T128).
 */
@Entity(tableName = "trash")
data class TrashEntity(
    @PrimaryKey val id: String,
    val originalPath: String,
    val name: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val deletedAtEpochMillis: Long,
)
