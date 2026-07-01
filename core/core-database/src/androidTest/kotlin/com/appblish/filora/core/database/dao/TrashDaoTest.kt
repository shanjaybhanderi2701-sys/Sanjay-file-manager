package com.appblish.filora.core.database.dao

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.appblish.filora.core.database.FiloraDatabase
import com.appblish.filora.core.database.entity.TrashEntity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Persistence guard for [TrashDao] (T123, T130, FR-3.4). Exercises the SQL behind the
 * recycle bin against a real in-memory Room database: newest-deleted-first ordering,
 * running total size (COALESCE over an empty table → 0), lookup/expiry queries, and
 * delete-by-id.
 */
@RunWith(AndroidJUnit4::class)
class TrashDaoTest {
    private lateinit var database: FiloraDatabase
    private lateinit var dao: TrashDao

    @Before
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder(
                    InstrumentationRegistry.getInstrumentation().targetContext,
                    FiloraDatabase::class.java,
                ).build()
        dao = database.trashDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun observeAll_emitsNewestDeletedFirst() =
        runBlocking {
            dao.upsert(entry(id = "1", name = "a.txt", deletedAt = 100))
            dao.upsert(entry(id = "2", name = "b.txt", deletedAt = 300))
            dao.upsert(entry(id = "3", name = "c.txt", deletedAt = 200))

            val ids = dao.observeAll().first().map { it.id }

            assertThat(ids).containsExactly("2", "3", "1").inOrder()
        }

    @Test
    fun observeTotalSize_sumsSizes_andIsZeroWhenEmpty() =
        runBlocking {
            assertThat(dao.observeTotalSize().first()).isEqualTo(0L)

            dao.upsert(entry(id = "1", size = 10, deletedAt = 100))
            dao.upsert(entry(id = "2", size = 25, deletedAt = 200))

            assertThat(dao.observeTotalSize().first()).isEqualTo(35L)
        }

    @Test
    fun findExpired_returnsOnlyRowsStrictlyBeforeCutoff() =
        runBlocking {
            dao.upsert(entry(id = "old", deletedAt = 100))
            dao.upsert(entry(id = "edge", deletedAt = 200))
            dao.upsert(entry(id = "fresh", deletedAt = 300))

            val expired = dao.findExpired(cutoffEpochMillis = 200).map { it.id }

            assertThat(expired).containsExactly("old")
        }

    @Test
    fun deleteById_removesOnlyThatEntry() =
        runBlocking {
            dao.upsert(entry(id = "1", deletedAt = 100))
            dao.upsert(entry(id = "2", deletedAt = 200))

            dao.deleteById("1")

            assertThat(dao.getAll().map { it.id }).containsExactly("2")
            assertThat(dao.findById("1")).isNull()
        }

    private fun entry(
        id: String,
        originalPath: String = "/sdcard/$id",
        name: String = "$id.txt",
        isDirectory: Boolean = false,
        size: Long = 1,
        deletedAt: Long,
    ) = TrashEntity(
        id = id,
        originalPath = originalPath,
        name = name,
        isDirectory = isDirectory,
        sizeBytes = size,
        deletedAtEpochMillis = deletedAt,
    )
}
