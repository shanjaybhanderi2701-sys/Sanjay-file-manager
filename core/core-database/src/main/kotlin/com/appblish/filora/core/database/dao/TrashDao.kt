package com.appblish.filora.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.appblish.filora.core.database.entity.TrashEntity
import kotlinx.coroutines.flow.Flow

/**
 * Persistence for the recycle bin (FR-3.4, T123). Rows are keyed by the opaque
 * trash id; the ordering column is the delete timestamp so the newest deletions lead
 * the list. Restore/permanent-delete/auto-purge remove rows by id (or by a retention
 * cutoff); the caller is responsible for the matching on-disk payload under the trash
 * directory before/after the row changes.
 */
@Dao
interface TrashDao {
    @Query("SELECT * FROM trash ORDER BY deletedAtEpochMillis DESC")
    fun observeAll(): Flow<List<TrashEntity>>

    /** Total footprint of the bin in bytes; 0 when empty (SUM is null → coalesced). */
    @Query("SELECT COALESCE(SUM(sizeBytes), 0) FROM trash")
    fun observeTotalSize(): Flow<Long>

    @Query("SELECT * FROM trash WHERE id = :id")
    suspend fun findById(id: String): TrashEntity?

    /** Rows older than [cutoffEpochMillis] (deleted-at strictly before the cutoff). */
    @Query("SELECT * FROM trash WHERE deletedAtEpochMillis < :cutoffEpochMillis")
    suspend fun findExpired(cutoffEpochMillis: Long): List<TrashEntity>

    @Query("SELECT * FROM trash")
    suspend fun getAll(): List<TrashEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: TrashEntity)

    @Query("DELETE FROM trash WHERE id = :id")
    suspend fun deleteById(id: String)
}
