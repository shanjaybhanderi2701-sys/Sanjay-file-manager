package com.appblish.filora.core.database.dao

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.appblish.filora.core.database.FiloraDatabase
import com.appblish.filora.core.database.entity.RecentEntity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Persistence guard for [RecentDao] (T164, FR-9.2). Exercises the DAO against a real
 * in-memory Room database: the most-recent-first ordering and `LIMIT`, REPLACE-on-
 * conflict dedup by path (re-opening a file moves it to the top without duplicating),
 * and the bulk `clear()`.
 */
@RunWith(AndroidJUnit4::class)
class RecentDaoTest {
    private lateinit var database: FiloraDatabase
    private lateinit var dao: RecentDao

    @Before
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder(
                    InstrumentationRegistry.getInstrumentation().targetContext,
                    FiloraDatabase::class.java,
                ).build()
        dao = database.recentDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun upsertThenObserve_emitsMostRecentlyOpenedFirst() =
        runBlocking {
            dao.upsert(recent(path = "/sdcard/a.txt", name = "a.txt", openedAt = 100))
            dao.upsert(recent(path = "/sdcard/c.txt", name = "c.txt", openedAt = 300))
            dao.upsert(recent(path = "/sdcard/b.txt", name = "b.txt", openedAt = 200))

            val recents = dao.observeRecent(limit = 10).first()

            // ORDER BY lastOpenedEpochMillis DESC → newest open leads.
            assertThat(recents.map { it.path })
                .containsExactly("/sdcard/c.txt", "/sdcard/b.txt", "/sdcard/a.txt")
                .inOrder()
        }

    @Test
    fun observeRecent_honoursLimit() =
        runBlocking {
            (1..5).forEach { i ->
                dao.upsert(recent(path = "/sdcard/$i.txt", name = "$i.txt", openedAt = i.toLong()))
            }

            val recents = dao.observeRecent(limit = 2).first()

            // Only the two most recent rows are returned, newest first.
            assertThat(recents.map { it.path }).containsExactly("/sdcard/5.txt", "/sdcard/4.txt").inOrder()
        }

    @Test
    fun upsertSamePath_replacesAndRefreshesTimestamp() =
        runBlocking {
            dao.upsert(recent(path = "/sdcard/a.txt", name = "a.txt", openedAt = 100))
            dao.upsert(recent(path = "/sdcard/a.txt", name = "a.txt", openedAt = 900))

            val recents = dao.observeRecent(limit = 10).first()

            assertThat(recents).hasSize(1)
            assertThat(recents.single().lastOpenedEpochMillis).isEqualTo(900)
        }

    @Test
    fun clear_removesEveryRecent() =
        runBlocking {
            dao.upsert(recent(path = "/sdcard/a.txt", name = "a.txt", openedAt = 100))
            dao.upsert(recent(path = "/sdcard/b.txt", name = "b.txt", openedAt = 200))

            dao.clear()

            assertThat(dao.observeRecent(limit = 10).first()).isEmpty()
        }

    private fun recent(
        path: String,
        name: String,
        isDirectory: Boolean = false,
        openedAt: Long,
    ) = RecentEntity(
        path = path,
        name = name,
        isDirectory = isDirectory,
        lastOpenedEpochMillis = openedAt,
    )
}
