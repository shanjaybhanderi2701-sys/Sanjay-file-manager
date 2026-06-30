package com.appblish.filora.core.domain.repository

import com.appblish.filora.core.domain.model.FileItem
import kotlinx.coroutines.flow.Flow

/** User-pinned favorites and recently-opened entries (Room-backed). */
interface FavoritesRepository {
    fun observeFavorites(): Flow<List<FileItem>>

    fun observeRecents(limit: Int = 20): Flow<List<FileItem>>

    suspend fun addFavorite(item: FileItem)

    suspend fun removeFavorite(path: String)

    suspend fun recordRecent(item: FileItem)
}
