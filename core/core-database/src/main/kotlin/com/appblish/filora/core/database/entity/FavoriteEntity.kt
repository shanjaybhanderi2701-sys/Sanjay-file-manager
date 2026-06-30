package com.appblish.filora.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A user-pinned file or folder. [path] is the stable identity. */
@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val path: String,
    val name: String,
    val isDirectory: Boolean,
    val addedAtEpochMillis: Long,
)
