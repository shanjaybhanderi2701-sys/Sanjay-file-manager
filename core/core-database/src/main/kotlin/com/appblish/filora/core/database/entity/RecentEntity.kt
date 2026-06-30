package com.appblish.filora.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A recently opened file/folder, deduplicated by [path]. */
@Entity(tableName = "recents")
data class RecentEntity(
    @PrimaryKey val path: String,
    val name: String,
    val isDirectory: Boolean,
    val lastOpenedEpochMillis: Long,
)
