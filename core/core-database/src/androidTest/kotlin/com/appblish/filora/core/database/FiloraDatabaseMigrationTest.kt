package com.appblish.filora.core.database

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Guards the exported Room schema. [MigrationTestHelper] opens each historical
 * schema version from the JSON exported under `core-database/schemas/` and Room
 * validates the on-disk schema against the compiled entities. At version 1 there
 * are no migrations, so this proves the baseline schema is created correctly and
 * gives future schema bumps a place to assert their migrations.
 */
@RunWith(AndroidJUnit4::class)
@Suppress("SpreadOperator") // Room's runMigrationsAndValidate/addMigrations take varargs.
class FiloraDatabaseMigrationTest {
    private val testDb = "filora-migration-test.db"

    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            FiloraDatabase::class.java,
        )

    @Test
    @Throws(IOException::class)
    fun createsVersion1Schema() {
        // Creating the DB at v1 validates the exported schema JSON exists and is
        // consistent; MigrationTestHelper throws if the schema file is missing.
        helper.createDatabase(testDb, 1).use { db ->
            assertThat(db.version).isEqualTo(1)
        }
    }

    @Test
    @Throws(IOException::class)
    fun migratesFromVersion1ToLatest() {
        // Seed at the earliest exported version using raw SQL against the v1 schema.
        helper.createDatabase(testDb, 1).use { db ->
            db.execSQL(
                "INSERT INTO favorites (path, name, isDirectory, addedAtEpochMillis) " +
                    "VALUES ('/sdcard/Docs', 'Docs', 1, 100)",
            )
            db.execSQL(
                "INSERT INTO recents (path, name, isDirectory, lastOpenedEpochMillis) " +
                    "VALUES ('/sdcard/a.txt', 'a.txt', 0, 200)",
            )
        }

        // Open with the generated implementation, running every registered migration.
        // Room validates the final schema matches the compiled entities, then we
        // confirm the v1 rows survived the open path.
        helper.runMigrationsAndValidate(testDb, 1, true, *FiloraDatabase.MIGRATIONS)

        val database =
            Room
                .databaseBuilder(
                    InstrumentationRegistry.getInstrumentation().targetContext,
                    FiloraDatabase::class.java,
                    testDb,
                ).addMigrations(*FiloraDatabase.MIGRATIONS)
                .build()

        try {
            val db = database.openHelper.writableDatabase
            db
                .query(
                    "SELECT path FROM favorites WHERE path = '/sdcard/Docs'",
                ).use { cursor ->
                    assertThat(cursor.count).isEqualTo(1)
                }
            db
                .query(
                    "SELECT path FROM recents WHERE path = '/sdcard/a.txt'",
                ).use { cursor ->
                    assertThat(cursor.count).isEqualTo(1)
                }
        } finally {
            database.close()
        }
    }

    @Test
    @Throws(IOException::class)
    fun migratesFromVersion1To2_addsTrashTable_preservingRows() {
        helper.createDatabase(testDb, 1).use { db ->
            db.execSQL(
                "INSERT INTO favorites (path, name, isDirectory, addedAtEpochMillis) " +
                    "VALUES ('/sdcard/Docs', 'Docs', 1, 100)",
            )
        }

        // Run only the v1→v2 migration and let Room validate the resulting schema
        // (including the new `trash` table) against the compiled entities.
        helper.runMigrationsAndValidate(testDb, 2, true, FiloraDatabase.MIGRATION_1_2).use { db ->
            // Pre-existing rows survive the additive migration.
            db.query("SELECT path FROM favorites WHERE path = '/sdcard/Docs'").use { cursor ->
                assertThat(cursor.count).isEqualTo(1)
            }
            // The new trash table exists and is writable/queryable.
            db.execSQL(
                "INSERT INTO trash (id, originalPath, name, isDirectory, sizeBytes, deletedAtEpochMillis) " +
                    "VALUES ('abc', '/sdcard/a.txt', 'a.txt', 0, 42, 500)",
            )
            db.query("SELECT sizeBytes FROM trash WHERE id = 'abc'").use { cursor ->
                assertThat(cursor.moveToFirst()).isTrue()
                assertThat(cursor.getLong(0)).isEqualTo(42)
            }
        }
    }
}
