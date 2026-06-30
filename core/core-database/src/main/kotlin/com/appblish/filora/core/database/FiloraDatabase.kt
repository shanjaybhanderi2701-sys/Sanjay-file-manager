package com.appblish.filora.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.appblish.filora.core.database.dao.FavoriteDao
import com.appblish.filora.core.database.dao.RecentDao
import com.appblish.filora.core.database.entity.FavoriteEntity
import com.appblish.filora.core.database.entity.RecentEntity

/**
 * Room database for Filora. Holds favorites and recents (and, later, the thumbnail
 * index). Schema export is deferred to the milestone that introduces migrations.
 */
@Database(
    entities = [FavoriteEntity::class, RecentEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class FiloraDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao

    abstract fun recentDao(): RecentDao

    companion object {
        const val NAME = "filora.db"
    }
}
