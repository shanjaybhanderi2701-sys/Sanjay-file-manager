package com.appblish.filora.core.domain.usecase

import com.appblish.filora.core.domain.model.FileItem
import com.appblish.filora.core.domain.repository.FavoritesRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ToggleFavoriteUseCaseTest {
    private fun file(path: String) =
        FileItem(
            name = path.substringAfterLast('/'),
            path = path,
            isDirectory = false,
            sizeBytes = 0L,
            lastModifiedEpochMillis = 0L,
        )

    /** In-memory favorites keyed by path; add/remove mutate the observable set. */
    private class FakeFavorites : FavoritesRepository {
        val favorites = MutableStateFlow<List<FileItem>>(emptyList())

        override fun observeFavorites(): Flow<List<FileItem>> = favorites

        override fun observeRecents(limit: Int): Flow<List<FileItem>> = throw UnsupportedOperationException()

        override suspend fun addFavorite(item: FileItem) {
            favorites.update { current -> if (current.any { it.path == item.path }) current else current + item }
        }

        override suspend fun removeFavorite(path: String) {
            favorites.update { current -> current.filterNot { it.path == path } }
        }

        override suspend fun recordRecent(item: FileItem) = Unit
    }

    @Test
    fun `pins an unpinned item and returns true`() =
        runTest {
            val repo = FakeFavorites()
            val item = file("/a/x.txt")

            val nowFavorite = ToggleFavoriteUseCase(repo)(item)

            assertThat(nowFavorite).isTrue()
            assertThat(repo.favorites.value.map { it.path }).containsExactly("/a/x.txt")
        }

    @Test
    fun `unpins an already-pinned item and returns false`() =
        runTest {
            val repo = FakeFavorites()
            val item = file("/a/x.txt")
            repo.favorites.value = listOf(item)

            val nowFavorite = ToggleFavoriteUseCase(repo)(item)

            assertThat(nowFavorite).isFalse()
            assertThat(repo.favorites.value).isEmpty()
        }

    @Test
    fun `matches by path so a re-created item still unpins`() =
        runTest {
            val repo = FakeFavorites()
            repo.favorites.value = listOf(file("/a/x.txt"))
            // Same path, different instance — toggling must still recognise it as pinned.
            val sameByPath = file("/a/x.txt")

            val nowFavorite = ToggleFavoriteUseCase(repo)(sameByPath)

            assertThat(nowFavorite).isFalse()
            assertThat(repo.favorites.value).isEmpty()
        }
}
