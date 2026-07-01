package com.appblish.filora.core.data.trash

import com.appblish.filora.core.common.result.Result
import com.appblish.filora.core.database.dao.TrashDao
import com.appblish.filora.core.database.entity.TrashEntity
import com.appblish.filora.core.domain.model.TrashRetention
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.time.Duration.Companion.days

/**
 * Unit tests for [AppTrashRepository] (T130, FR-3.4) over a temp filesystem and an
 * in-memory fake [TrashDao] (no Robolectric in the catalog). They pin the round-trip
 * that matters for safe delete: move-to-trash relocates the bytes and records
 * metadata, restore returns them to the original path, permanent delete/empty remove
 * both payload and row, size accounting sums the rows, and auto-purge only removes
 * items older than the retention window.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppTrashRepositoryTest {
    @get:Rule
    val temp = TemporaryFolder()

    /** In-memory [TrashDao]: an id-keyed map exposed newest-deleted first. */
    private class FakeTrashDao : TrashDao {
        val rows = MutableStateFlow<Map<String, TrashEntity>>(emptyMap())

        override fun observeAll(): Flow<List<TrashEntity>> =
            rows.map { it.values.sortedByDescending(TrashEntity::deletedAtEpochMillis) }

        override fun observeTotalSize(): Flow<Long> =
            rows.map { map -> map.values.sumOf { it.sizeBytes } }

        override suspend fun findById(id: String): TrashEntity? = rows.value[id]

        override suspend fun findExpired(cutoffEpochMillis: Long): List<TrashEntity> =
            rows.value.values.filter { it.deletedAtEpochMillis < cutoffEpochMillis }

        override suspend fun getAll(): List<TrashEntity> = rows.value.values.toList()

        override suspend fun upsert(entry: TrashEntity) {
            rows.value = rows.value + (entry.id to entry)
        }

        override suspend fun deleteById(id: String) {
            rows.value = rows.value - id
        }
    }

    private lateinit var dao: FakeTrashDao
    private lateinit var trashDir: File
    private lateinit var userDir: File
    private var clock = 1_000L

    private fun repository() =
        AppTrashRepository(
            trashDao = dao,
            trashDir = trashDir,
            ioDispatcher = UnconfinedTestDispatcher(),
            now = { clock },
        )

    private fun setUpDirs() {
        dao = FakeTrashDao()
        trashDir = File(temp.root, "trash")
        userDir = temp.newFolder("user")
    }

    private fun userFile(
        name: String,
        content: String = "hello",
    ): File =
        File(userDir, name).apply { writeText(content) }

    @Test
    fun `canTrash accepts local paths and rejects content uris`() {
        setUpDirs()
        val repo = repository()
        assertThat(repo.canTrash("/sdcard/a.txt")).isTrue()
        assertThat(repo.canTrash("content://tree/doc")).isFalse()
        assertThat(repo.canTrash("")).isFalse()
    }

    @Test
    fun `moveToTrash relocates bytes and records metadata`() =
        runTest {
            setUpDirs()
            val repo = repository()
            val file = userFile("a.txt", content = "1234")

            val result = repo.moveToTrash(listOf(file.path))

            assertThat(result).isEqualTo(Result.Success(1))
            // Original is gone; the byte payload now lives under the trash dir.
            assertThat(file.exists()).isFalse()
            val row = dao.getAll().single()
            assertThat(row.originalPath).isEqualTo(file.path)
            assertThat(row.name).isEqualTo("a.txt")
            assertThat(row.sizeBytes).isEqualTo(4)
            assertThat(row.deletedAtEpochMillis).isEqualTo(1_000L)
            assertThat(File(trashDir, row.id).readText()).isEqualTo("1234")
        }

    @Test
    fun `moveToTrash skips a missing source and counts only real moves`() =
        runTest {
            setUpDirs()
            val repo = repository()
            val file = userFile("a.txt")

            val result = repo.moveToTrash(listOf(file.path, "/does/not/exist.txt"))

            assertThat(result).isEqualTo(Result.Success(1))
            assertThat(dao.getAll()).hasSize(1)
        }

    @Test
    fun `restore returns the item to its original path and clears the row`() =
        runTest {
            setUpDirs()
            val repo = repository()
            val file = userFile("a.txt", content = "keep-me")
            repo.moveToTrash(listOf(file.path))
            val id = dao.getAll().single().id

            val result = repo.restore(listOf(id))

            assertThat(result).isEqualTo(Result.Success(1))
            assertThat(file.exists()).isTrue()
            assertThat(file.readText()).isEqualTo("keep-me")
            assertThat(dao.getAll()).isEmpty()
            assertThat(File(trashDir, id).exists()).isFalse()
        }

    @Test
    fun `restore refuses to clobber an existing file and leaves it in the bin`() =
        runTest {
            setUpDirs()
            val repo = repository()
            val file = userFile("a.txt")
            repo.moveToTrash(listOf(file.path))
            val id = dao.getAll().single().id
            // Something else now occupies the original path.
            userFile("a.txt", content = "new")

            val result = repo.restore(listOf(id))

            assertThat(result).isEqualTo(Result.Success(0))
            assertThat(dao.getAll()).hasSize(1) // still in the bin
            assertThat(File(trashDir, id).exists()).isTrue()
        }

    @Test
    fun `deleteForever removes payload and row`() =
        runTest {
            setUpDirs()
            val repo = repository()
            val file = userFile("a.txt")
            repo.moveToTrash(listOf(file.path))
            val id = dao.getAll().single().id

            val result = repo.deleteForever(listOf(id))

            assertThat(result).isEqualTo(Result.Success(1))
            assertThat(dao.getAll()).isEmpty()
            assertThat(File(trashDir, id).exists()).isFalse()
        }

    @Test
    fun `emptyTrash removes everything`() =
        runTest {
            setUpDirs()
            val repo = repository()
            repo.moveToTrash(listOf(userFile("a.txt").path, userFile("b.txt").path))
            assertThat(dao.getAll()).hasSize(2)

            val result = repo.emptyTrash()

            assertThat(result).isEqualTo(Result.Success(2))
            assertThat(dao.getAll()).isEmpty()
            assertThat(trashDir.listFiles().orEmpty()).isEmpty()
        }

    @Test
    fun `observeTrashSize sums the trashed sizes`() =
        runTest {
            setUpDirs()
            val repo = repository()
            repo.moveToTrash(listOf(userFile("a.txt", content = "12").path))
            repo.moveToTrash(listOf(userFile("b.txt", content = "12345").path))

            assertThat(repo.observeTrashSize().first()).isEqualTo(7L)
        }

    @Test
    fun `purgeExpired removes only items older than the retention window`() =
        runTest {
            setUpDirs()
            val repo = repository()
            // Old item deleted at t=1000.
            clock = 1_000L
            repo.moveToTrash(listOf(userFile("old.txt").path))
            val oldId = dao.getAll().single().id
            // Fresh item deleted "now" — 10 days later.
            clock = 1_000L + 10.days.inWholeMilliseconds
            repo.moveToTrash(listOf(userFile("fresh.txt").path))

            // Retention is 5 days; only the 10-day-old item is expired.
            val result = repo.purgeExpired(TrashRetention(maxAge = 5.days))

            assertThat(result).isEqualTo(Result.Success(1))
            assertThat(dao.getAll().map { it.id }).doesNotContain(oldId)
            assertThat(dao.getAll()).hasSize(1)
        }

    @Test
    fun `moveToTrash then restore round-trips a directory tree`() =
        runTest {
            setUpDirs()
            val repo = repository()
            val dir = File(userDir, "folder").apply { mkdirs() }
            File(dir, "nested.txt").writeText("deep")

            repo.moveToTrash(listOf(dir.path))
            assertThat(dir.exists()).isFalse()
            val row = dao.getAll().single()
            assertThat(row.isDirectory).isTrue()

            repo.restore(listOf(row.id))

            assertThat(File(dir, "nested.txt").readText()).isEqualTo("deep")
        }
}
