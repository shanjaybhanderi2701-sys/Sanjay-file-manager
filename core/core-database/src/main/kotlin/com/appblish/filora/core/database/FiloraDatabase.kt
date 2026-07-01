package com.appblish.filora.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.appblish.filora.core.database.dao.FavoriteDao
import com.appblish.filora.core.database.dao.RecentDao
import com.appblish.filora.core.database.dao.TrashDao
import com.appblish.filora.core.database.entity.FavoriteEntity
import com.appblish.filora.core.database.entity.RecentEntity
import com.appblish.filora.core.database.entity.TrashEntity

/**
 * Room database for Filora. Holds favorites, recents and the recycle-bin index (and,
 * later, the thumbnail index). The schema is exported to `core-database/schemas`
 * (configured by the Room convention plugin) so migration tests can validate each
 * version and future schema bumps stay guarded.
 */
@Database(
    entities = [FavoriteEntity::class, RecentEntity::class, TrashEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class FiloraDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao

    abstract fun recentDao(): RecentDao

    abstract fun trashDao(): TrashDao

    companion object {
        const val NAME = "filora.db"

        /**
         * v1 → v2 (M12 recycle bin, T123): adds the `trash` table. Purely additive —
         * existing favorites/recents rows are untouched — so the migration only
         * creates the new table. The `CREATE TABLE` mirrors the [TrashEntity] schema
         * Room generates; the migration test validates it against the compiled entity.
         */
        val MIGRATION_1_2: Migration =
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `trash` (" +
                            "`id` TEXT NOT NULL, " +
                            "`originalPath` TEXT NOT NULL, " +
                            "`name` TEXT NOT NULL, " +
                            "`isDirectory` INTEGER NOT NULL, " +
                            "`sizeBytes` INTEGER NOT NULL, " +
                            "`deletedAtEpochMillis` INTEGER NOT NULL, " +
                            "PRIMARY KEY(`id`))",
                    )
                }
            }

        /**
         * Ordered manual migrations applied when opening the database. Every schema
         * bump appends its `Migration(from, to)` here and adds a case to the
         * migration test.
         */
        val MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2)
    }
}
