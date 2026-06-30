package com.appblish.filora.core.data.favorites

import com.appblish.filora.core.database.dao.FavoriteDao
import com.appblish.filora.core.database.dao.RecentDao
import com.appblish.filora.core.database.entity.FavoriteEntity
import com.appblish.filora.core.database.entity.RecentEntity
import com.appblish.filora.core.domain.model.FileItem
import com.appblish.filora.core.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Room-backed [FavoritesRepository] (FR-9.1 favorites, FR-9.2 recents).
 *
 * Both tables key on [FileItem.path] as the stable identity: pinning the same path
 * twice is idempotent, and re-opening a file refreshes its recents timestamp via the
 * DAO's REPLACE upsert rather than duplicating the row. Only the identity columns
 * (path/name/isDirectory) and the relevant timestamp are persisted — the volatile
 * [FileItem] fields (size, mime, child count) are intentionally not stored, since a
 * pinned or recent entry is re-resolved against the live filesystem when opened. On
 * read those fields rehydrate to their defaults; the timestamp column doubles as the
 * item's [FileItem.lastModifiedEpochMillis] so the UI can still sort/label.
 *
 * [now] lets tests pin timestamps; the Hilt-used constructor binds it to the wall
 * clock (Dagger can't synthesize a `() -> Long`, hence the secondary `@Inject` one).
 */
class FavoritesRepositoryImpl(
    private val favoriteDao: FavoriteDao,
    private val recentDao: RecentDao,
    private val now: () -> Long,
) : FavoritesRepository {
    @Inject
    constructor(
        favoriteDao: FavoriteDao,
        recentDao: RecentDao,
    ) : this(favoriteDao, recentDao, { System.currentTimeMillis() })

    override fun observeFavorites(): Flow<List<FileItem>> =
        favoriteDao.observeAll().map { entities -> entities.map(FavoriteEntity::toFileItem) }

    override fun observeRecents(limit: Int): Flow<List<FileItem>> =
        recentDao.observeRecent(limit).map { entities -> entities.map(RecentEntity::toFileItem) }

    override suspend fun addFavorite(item: FileItem) {
        favoriteDao.upsert(
            FavoriteEntity(
                path = item.path,
                name = item.name,
                isDirectory = item.isDirectory,
                addedAtEpochMillis = now(),
            ),
        )
    }

    override suspend fun removeFavorite(path: String) {
        favoriteDao.deleteByPath(path)
    }

    override suspend fun recordRecent(item: FileItem) {
        recentDao.upsert(
            RecentEntity(
                path = item.path,
                name = item.name,
                isDirectory = item.isDirectory,
                lastOpenedEpochMillis = now(),
            ),
        )
    }
}

private fun FavoriteEntity.toFileItem(): FileItem =
    FileItem(
        name = name,
        path = path,
        isDirectory = isDirectory,
        sizeBytes = 0L,
        lastModifiedEpochMillis = addedAtEpochMillis,
    )

private fun RecentEntity.toFileItem(): FileItem =
    FileItem(
        name = name,
        path = path,
        isDirectory = isDirectory,
        sizeBytes = 0L,
        lastModifiedEpochMillis = lastOpenedEpochMillis,
    )
