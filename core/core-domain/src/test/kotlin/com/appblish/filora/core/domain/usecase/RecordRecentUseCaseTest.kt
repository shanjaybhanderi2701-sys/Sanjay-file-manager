package com.appblish.filora.core.domain.usecase

import com.appblish.filora.core.domain.model.FileItem
import com.appblish.filora.core.domain.repository.FavoritesRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RecordRecentUseCaseTest {
    private fun file(path: String) =
        FileItem(
            name = path.substringAfterLast('/'),
            path = path,
            isDirectory = false,
            sizeBytes = 0L,
            lastModifiedEpochMillis = 0L,
        )

    /** Captures every item the use case forwards to be recorded as recent. */
    private class FakeFavorites : FavoritesRepository {
        val recorded = mutableListOf<FileItem>()

        override fun observeFavorites(): Flow<List<FileItem>> = throw UnsupportedOperationException()

        override fun observeRecents(limit: Int): Flow<List<FileItem>> = throw UnsupportedOperationException()

        override suspend fun addFavorite(item: FileItem) = Unit

        override suspend fun removeFavorite(path: String) = Unit

        override suspend fun recordRecent(item: FileItem) {
            recorded += item
        }
    }

    @Test
    fun `forwards the opened item to the repository`() =
        runTest {
            val repo = FakeFavorites()
            val item = file("/a/opened.txt")

            RecordRecentUseCase(repo)(item)

            assertThat(repo.recorded).containsExactly(item)
        }
}
