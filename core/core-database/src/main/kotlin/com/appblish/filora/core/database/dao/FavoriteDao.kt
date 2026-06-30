package com.appblish.filora.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.appblish.filora.core.database.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY addedAtEpochMillis DESC")
    fun observeAll(): Flow<List<FavoriteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE path = :path")
    suspend fun deleteByPath(path: String)
}
