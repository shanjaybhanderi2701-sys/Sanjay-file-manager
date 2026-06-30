package com.appblish.filora.core.data.favorites

import com.appblish.filora.core.database.dao.FavoriteDao
import com.appblish.filora.core.database.dao.RecentDao
import com.appblish.filora.core.database.entity.FavoriteEntity
import com.appblish.filora.core.database.entity.RecentEntity
import com.appblish.filora.core.domain.model.FileItem
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Unit tests for [FavoritesRepositoryImpl] over in-memory fake DAOs (no Robolectric
 * in the catalog). They pin the entity↔[FileItem] mapping, path-keyed idempotency
 * for both tables (FR-9.1/FR-9.2), the recents dedup-on-reopen, and that a fixed
 * clock stamps the rows.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesRepositoryImplTest {
    /** In-memory [FavoriteDao]: a path-keyed map exposed newest-added first. */
    private class FakeFavoriteDao : FavoriteDao {
        private val rows = MutableStateFlow<Map<String, FavoriteEntity>>(emptyMap())

        override fun observeAll(): Flow<List<FavoriteEntity>> =
            rows.map { it.values.sortedByDescending(FavoriteEntity::addedAtEpochMillis) }

        override suspend fun upsert(favorite: FavoriteEntity) {
            rows.value = rows.value + (favorite.path to favorite)
        }

        override suspend fun deleteByPath(path: String) {
            rows.value = rows.value - path
        }
    }

    /** In-memory [RecentDao]: a path-keyed map, newest-opened first, capped by limit. */
    private class FakeRecentDao : RecentDao {
        private val rows = MutableStateFlow<Map<String, RecentEntity>>(emptyMap())

        override fun observeRecent(limit: Int): Flow<List<RecentEntity>> =
            rows.map { map ->
                map.values.sortedByDescending(RecentEntity::lastOpenedEpochMillis).take(limit)
            }

        override suspend fun upsert(recent: RecentEntity) {
            rows.value = rows.value + (recent.path to recent)
        }

        override suspend fun clear() {
            rows.value = emptyMap()
        }
    }

    private fun repository(now: () -> Long) = FavoritesRepositoryImpl(FakeFavoriteDao(), FakeRecentDao(), now)

    private fun file(
        path: String,
        isDirectory: Boolean = false,
    ) = FileItem(
        name = path.substringAfterLast('/'),
        path = path,
        isDirectory = isDirectory,
        sizeBytes = 123L,
        lastModifiedEpochMillis = 999L,
    )

    @Test
    fun `addFavorite persists and maps back to a FileItem`() =
        runTest {
            val repo = repository(now = { 42L })

            repo.addFavorite(file("/docs/report.pdf"))

            val favorites = repo.observeFavorites().first()
            assertThat(favorites).hasSize(1)
            val item = favorites.single()
            assertThat(item.path).isEqualTo("/docs/report.pdf")
            assertThat(item.name).isEqualTo("report.pdf")
            assertThat(item.isDirectory).isFalse()
            // The added-at clock doubles as the item's lastModified for UI sorting.
            assertThat(item.lastModifiedEpochMillis).isEqualTo(42L)
        }

    @Test
    fun `addFavorite is idempotent on path`() =
        runTest {
            val repo = repository(now = { 1L })

            repo.addFavorite(file("/a/x.txt"))
            repo.addFavorite(file("/a/x.txt"))

            assertThat(repo.observeFavorites().first()).hasSize(1)
        }

    @Test
    fun `removeFavorite drops the pinned path`() =
        runTest {
            val repo = repository(now = { 1L })
            repo.addFavorite(file("/a/x.txt"))
            repo.addFavorite(file("/a/y.txt"))

            repo.removeFavorite("/a/x.txt")

            assertThat(repo.observeFavorites().first().map { it.path }).containsExactly("/a/y.txt")
        }

    @Test
    fun `recordRecent dedups by path and refreshes the timestamp`() =
        runTest {
            var clock = 100L
            val repo = repository(now = { clock })

            repo.recordRecent(file("/a/song.mp3"))
            clock = 200L
            repo.recordRecent(file("/a/song.mp3"))

            val recents = repo.observeRecents(limit = 10).first()
            assertThat(recents).hasSize(1)
            assertThat(recents.single().lastModifiedEpochMillis).isEqualTo(200L)
        }

    @Test
    fun `observeRecents orders newest-first and honours the limit`() =
        runTest {
            var clock = 1L
            val repo = repository(now = { clock })
            repo.recordRecent(file("/a/1"))
            clock = 2L
            repo.recordRecent(file("/a/2"))
            clock = 3L
            repo.recordRecent(file("/a/3"))

            val recents = repo.observeRecents(limit = 2).first()
            assertThat(recents.map { it.path }).containsExactly("/a/3", "/a/2").inOrder()
        }
}
