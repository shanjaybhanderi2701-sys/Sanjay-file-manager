package com.appblish.filora.core.database.dao

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.appblish.filora.core.database.FiloraDatabase
import com.appblish.filora.core.database.entity.FavoriteEntity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Persistence guard for [FavoriteDao] (T096, FR-9.1). Exercises the DAO against a real
 * in-memory Room database so the SQL behind pin/unpin is verified, not just the
 * repository wrapper: insert + observe ordering, REPLACE-on-conflict (re-pinning the
 * same path is idempotent and refreshes its timestamp), and delete-by-path (unpin).
 */
@RunWith(AndroidJUnit4::class)
class FavoriteDaoTest {
    private lateinit var database: FiloraDatabase
    private lateinit var dao: FavoriteDao

    @Before
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder(
                    InstrumentationRegistry.getInstrumentation().targetContext,
                    FiloraDatabase::class.java,
                ).build()
        dao = database.favoriteDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun upsertThenObserve_emitsNewestPinnedFirst() =
        runBlocking {
            dao.upsert(favorite(path = "/sdcard/a.txt", name = "a.txt", addedAt = 100))
            dao.upsert(favorite(path = "/sdcard/Docs", name = "Docs", isDirectory = true, addedAt = 300))
            dao.upsert(favorite(path = "/sdcard/b.txt", name = "b.txt", addedAt = 200))

            val favorites = dao.observeAll().first()

            // ORDER BY addedAtEpochMillis DESC → newest pin leads.
            assertThat(favorites.map { it.path })
                .containsExactly("/sdcard/Docs", "/sdcard/b.txt", "/sdcard/a.txt")
                .inOrder()
        }

    @Test
    fun upsertSamePath_replacesRatherThanDuplicating() =
        runBlocking {
            dao.upsert(favorite(path = "/sdcard/a.txt", name = "a.txt", addedAt = 100))
            dao.upsert(favorite(path = "/sdcard/a.txt", name = "a.txt", addedAt = 500))

            val favorites = dao.observeAll().first()

            assertThat(favorites).hasSize(1)
            assertThat(favorites.single().addedAtEpochMillis).isEqualTo(500)
        }

    // Explicit `: Unit` — the trailing `containsExactly(...)` returns Truth's `Ordered`,
    // which as an expression body would make this @Test method non-void and JUnit4 would
    // reject the whole class with InvalidTestClassError (APP-150).
    @Test
    fun deleteByPath_removesOnlyThatFavorite(): Unit =
        runBlocking {
            dao.upsert(favorite(path = "/sdcard/a.txt", name = "a.txt", addedAt = 100))
            dao.upsert(favorite(path = "/sdcard/b.txt", name = "b.txt", addedAt = 200))

            dao.deleteByPath("/sdcard/a.txt")

            val favorites = dao.observeAll().first()
            assertThat(favorites.map { it.path }).containsExactly("/sdcard/b.txt")
        }

    @Test
    fun deleteByPath_unknownPath_isNoOp() =
        runBlocking {
            dao.upsert(favorite(path = "/sdcard/a.txt", name = "a.txt", addedAt = 100))

            dao.deleteByPath("/sdcard/missing")

            assertThat(dao.observeAll().first()).hasSize(1)
        }

    private fun favorite(
        path: String,
        name: String,
        isDirectory: Boolean = false,
        addedAt: Long,
    ) = FavoriteEntity(
        path = path,
        name = name,
        isDirectory = isDirectory,
        addedAtEpochMillis = addedAt,
    )
}
