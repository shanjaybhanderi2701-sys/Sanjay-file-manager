package com.appblish.filora.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import com.appblish.filora.core.database.dao.FavoriteDao
import com.appblish.filora.core.database.dao.RecentDao
import com.appblish.filora.core.database.entity.FavoriteEntity
import com.appblish.filora.core.database.entity.RecentEntity

/**
 * Room database for Filora. Holds favorites and recents (and, later, the thumbnail
 * index). The schema is exported to `core-database/schemas` (configured by the Room
 * convention plugin) so migration tests can validate each version and future schema
 * bumps stay guarded.
 */
@Database(
    entities = [FavoriteEntity::class, RecentEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class FiloraDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao

    abstract fun recentDao(): RecentDao

    companion object {
        const val NAME = "filora.db"

        /**
         * Ordered manual migrations applied when opening the database. Empty at
         * version 1; every schema bump appends its `Migration(from, to)` here and
         * adds a case to the migration test.
         */
        val MIGRATIONS: Array<Migration> = emptyArray()
    }
}
