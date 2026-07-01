package com.appblish.filora.core.domain.repository

import com.appblish.filora.core.common.result.Result
import com.appblish.filora.core.domain.model.TrashRetention
import com.appblish.filora.core.domain.model.TrashedItem
import kotlinx.coroutines.flow.Flow

/**
 * The app-managed recycle bin (FR-3.4, M12). Deleting a local file "to trash" moves
 * its bytes into an app-private trash directory and records metadata; restore moves
 * them back to the original location; permanent delete drops both. The bin lets the
 * user recover from accidental deletes (NFR-2.4) with a bounded footprint governed by
 * the retention/auto-purge policy (T128).
 *
 * **Scope:** only locations Filora can move on the filesystem are trashable — see
 * [canTrash]. `content://` (SAF/MediaStore) items are deleted permanently by the
 * [FileRepository], since moving them into an app-private directory isn't reliably
 * possible under scoped storage.
 *
 * **Failure model:** items that are simply absent (a source that no longer exists, an
 * unknown/orphaned id) are skipped and the batch continues; a hard I/O failure aborts
 * the batch and surfaces an error. Every mutating call reports the number of items
 * actually affected.
 */
interface TrashRepository {
    /** Streams the bin contents, newest deletion first, re-emitting on change. */
    fun observeTrash(): Flow<List<TrashedItem>>

    /** Streams the bin's total on-disk footprint in bytes (0 when empty). */
    fun observeTrashSize(): Flow<Long>

    /** True when [path] is a location Filora can move into the trash directory. */
    fun canTrash(path: String): Boolean

    /**
     * Moves [paths] into the trash (each must satisfy [canTrash]). Returns the number
     * of items moved. On any per-item failure that item is skipped; the rest proceed,
     * so a partial success still frees the user's storage — the returned count tells
     * the caller how many made it.
     */
    suspend fun moveToTrash(paths: List<String>): Result<Int>

    /**
     * Restores the trashed items with the given [ids] back to their original paths,
     * recreating missing parent directories. An item whose original name is now taken
     * fails with [com.appblish.filora.core.common.result.OperationError.Conflict] and
     * is left in the bin. Returns the number restored.
     */
    suspend fun restore(ids: List<String>): Result<Int>

    /** Permanently deletes the trashed items with the given [ids]. Returns the count. */
    suspend fun deleteForever(ids: List<String>): Result<Int>

    /** Permanently deletes everything in the bin. Returns the number removed. */
    suspend fun emptyTrash(): Result<Int>

    /**
     * Auto-purge (T128): permanently deletes items whose delete timestamp is older
     * than [retention]'s window. Safe to call on app start / bin open. Returns the
     * number purged.
     */
    suspend fun purgeExpired(retention: TrashRetention = TrashRetention.Default): Result<Int>
}
