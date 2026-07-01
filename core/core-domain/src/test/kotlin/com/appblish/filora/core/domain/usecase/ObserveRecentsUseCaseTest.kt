package com.appblish.filora.core.domain.usecase

import app.cash.turbine.test
import com.appblish.filora.core.domain.model.FileItem
import com.appblish.filora.core.domain.repository.FavoritesRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ObserveRecentsUseCaseTest {
    private fun file(path: String) =
        FileItem(
            name = path.substringAfterLast('/'),
            path = path,
            isDirectory = false,
            sizeBytes = 0L,
            lastModifiedEpochMillis = 0L,
        )

    /** Records the limit each recents subscription was opened with. */
    private class FakeFavorites : FavoritesRepository {
        val recents = MutableStateFlow<List<FileItem>>(emptyList())
        var observedLimit: Int? = null

        override fun observeFavorites(): Flow<List<FileItem>> = throw UnsupportedOperationException()

        override fun observeRecents(limit: Int): Flow<List<FileItem>> {
            observedLimit = limit
            return recents
        }

        override suspend fun addFavorite(item: FileItem) = Unit

        override suspend fun removeFavorite(path: String) = Unit

        override suspend fun recordRecent(item: FileItem) = Unit
    }

    @Test
    fun `streams recents newest-first and forwards the default limit`() =
        runTest {
            val repo = FakeFavorites()
            repo.recents.value = listOf(file("/a/opened.txt"))

            ObserveRecentsUseCase(repo)().test {
                assertThat(awaitItem().map { it.path }).containsExactly("/a/opened.txt")

                repo.recents.value = listOf(file("/b/newer.txt"), file("/a/opened.txt"))
                assertThat(awaitItem().map { it.path })
                    .containsExactly("/b/newer.txt", "/a/opened.txt")
                    .inOrder()

                cancelAndIgnoreRemainingEvents()
            }

            assertThat(repo.observedLimit).isEqualTo(ObserveRecentsUseCase.DEFAULT_LIMIT)
        }

    @Test
    fun `forwards an explicit limit to the repository`() =
        runTest {
            val repo = FakeFavorites()

            ObserveRecentsUseCase(repo)(limit = 5).test {
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }

            assertThat(repo.observedLimit).isEqualTo(5)
        }
}
