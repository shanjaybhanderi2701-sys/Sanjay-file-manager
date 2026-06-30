package com.appblish.filora.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.appblish.filora.core.database.entity.RecentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentDao {
    @Query("SELECT * FROM recents ORDER BY lastOpenedEpochMillis DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<RecentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(recent: RecentEntity)

    @Query("DELETE FROM recents")
    suspend fun clear()
}
