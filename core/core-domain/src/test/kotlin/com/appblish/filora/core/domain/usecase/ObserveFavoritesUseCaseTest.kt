package com.appblish.filora.core.domain.usecase

import app.cash.turbine.test
import com.appblish.filora.core.domain.model.FileItem
import com.appblish.filora.core.domain.repository.FavoritesRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ObserveFavoritesUseCaseTest {
    private fun file(path: String) =
        FileItem(
            name = path.substringAfterLast('/'),
            path = path,
            isDirectory = false,
            sizeBytes = 0L,
            lastModifiedEpochMillis = 0L,
        )

    /** In-memory favorites keyed by path, exposed as an observable list. */
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
    fun `streams the repository favorites and re-emits as the set changes`() =
        runTest {
            val repo = FakeFavorites()
            repo.favorites.value = listOf(file("/a/pinned.txt"))

            ObserveFavoritesUseCase(repo)().test {
                assertThat(awaitItem().map { it.path }).containsExactly("/a/pinned.txt")

                repo.addFavorite(file("/b/star.txt"))
                assertThat(awaitItem().map { it.path })
                    .containsExactly("/a/pinned.txt", "/b/star.txt")
                    .inOrder()

                repo.removeFavorite("/a/pinned.txt")
                assertThat(awaitItem().map { it.path }).containsExactly("/b/star.txt")

                cancelAndIgnoreRemainingEvents()
            }
        }
}
