package com.appblish.filora.core.data.trash

import android.content.Context
import com.appblish.filora.core.common.dispatcher.IoDispatcher
import com.appblish.filora.core.common.result.OperationError
import com.appblish.filora.core.common.result.Result
import com.appblish.filora.core.common.result.runCatchingResult
import com.appblish.filora.core.database.dao.TrashDao
import com.appblish.filora.core.database.entity.TrashEntity
import com.appblish.filora.core.domain.model.TrashRetention
import com.appblish.filora.core.domain.model.TrashedItem
import com.appblish.filora.core.domain.repository.TrashRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.inject.Inject

/**
 * App-managed recycle bin (T122, FR-3.4). Deleting a local file "to trash" moves its
 * bytes under an app-private trash directory keyed by an opaque UUID and records the
 * metadata needed to list/size/restore/purge it in the [TrashDao].
 *
 * **Where the bytes live (T122).** The trash directory sits under the app's
 * external-files dir (`Android/data/<pkg>/files/trash`) with an internal fallback.
 * That location is on the same volume as most user files, so moving an item into (or
 * out of) the bin is a fast `renameTo`; a cross-volume delete degrades to
 * copy-then-delete. It is app-private, so no runtime permission is needed and the OS
 * reclaims it on uninstall — an acceptable trade for a recycle bin.
 *
 * **Safety.** Restore recreates missing parent directories and refuses to clobber an
 * existing file at the original path (that item stays in the bin). Mutating batches
 * are per-item best-effort — one failing item is skipped and the rest proceed — and
 * each returns how many items were actually affected.
 *
 * [now] and the primary constructor's explicit [trashDir] let tests drive the clock
 * and point the bin at a temp directory; the Hilt [Inject] constructor derives both
 * from the application context (Dagger can't synthesize a `() -> Long`).
 */
class AppTrashRepository(
    private val trashDao: TrashDao,
    private val trashDir: File,
    private val ioDispatcher: CoroutineDispatcher,
    private val now: () -> Long,
) : TrashRepository {
    @Inject
    constructor(
        trashDao: TrashDao,
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ) : this(trashDao, resolveTrashDir(context), ioDispatcher, { System.currentTimeMillis() })

    override fun observeTrash(): Flow<List<TrashedItem>> =
        trashDao.observeAll().map { rows -> rows.map(TrashEntity::toTrashedItem) }

    override fun observeTrashSize(): Flow<Long> = trashDao.observeTotalSize()

    override fun canTrash(path: String): Boolean = path.isNotBlank() && !path.startsWith(CONTENT_SCHEME)

    override suspend fun moveToTrash(paths: List<String>): Result<Int> =
        io {
            ensureTrashDir()
            paths.count { path -> moveOneToTrash(path) }
        }

    override suspend fun restore(ids: List<String>): Result<Int> =
        io {
            ids.count { id -> restoreOne(id) }
        }

    override suspend fun deleteForever(ids: List<String>): Result<Int> =
        io {
            ids.count { id -> deleteOneForever(id) }
        }

    override suspend fun emptyTrash(): Result<Int> =
        io {
            trashDao.getAll().count { entry -> deleteOneForever(entry.id) }
        }

    override suspend fun purgeExpired(retention: TrashRetention): Result<Int> =
        io {
            val cutoff = now() - retention.maxAge.inWholeMilliseconds
            trashDao.findExpired(cutoff).count { entry -> deleteOneForever(entry.id) }
        }

    /** Moves [path] into the bin, recording metadata. Returns false if the source is gone. */
    private suspend fun moveOneToTrash(path: String): Boolean {
        val source = File(path)
        if (!source.exists()) return false
        val id = UUID.randomUUID().toString()
        val payload = File(trashDir, id)
        val size = source.sizeRecursive()
        moveTree(source, payload)
        trashDao.upsert(
            TrashEntity(
                id = id,
                originalPath = path,
                name = source.name,
                isDirectory = payload.isDirectory,
                sizeBytes = size,
                deletedAtEpochMillis = now(),
            ),
        )
        return true
    }

    /** Restores the item with [id] to its original path. Returns false if it can't (missing / name taken). */
    private suspend fun restoreOne(id: String): Boolean {
        val entry = trashDao.findById(id) ?: return false
        val payload = File(trashDir, id)
        val target = File(entry.originalPath)
        if (!payload.exists()) {
            // Payload lost (e.g. manual cleanup); drop the orphaned row so the bin stays honest.
            trashDao.deleteById(id)
            return false
        }
        if (target.exists()) return false // don't clobber; leave it in the bin (Conflict).
        target.parentFile?.mkdirs()
        moveTree(payload, target)
        trashDao.deleteById(id)
        return true
    }

    /** Permanently removes the item with [id] (payload + row). Returns false if unknown. */
    private suspend fun deleteOneForever(id: String): Boolean {
        val entry = trashDao.findById(id) ?: return false
        val payload = File(trashDir, id)
        if (payload.exists() && !payload.deleteRecursively()) {
            throw IOException("Cannot delete trashed item: $id")
        }
        trashDao.deleteById(id)
        return true
    }

    private fun ensureTrashDir() {
        if (!trashDir.exists() && !trashDir.mkdirs()) {
            throw IOException("Cannot create trash directory: $trashDir")
        }
    }

    /** Moves [source] to [dest]: an in-volume rename when possible, else copy-then-delete. */
    private fun moveTree(
        source: File,
        dest: File,
    ) {
        if (source.renameTo(dest)) return
        source.copyRecursively(dest, overwrite = true)
        if (!source.deleteRecursively()) {
            throw IOException("Cannot remove source after copy: ${source.path}")
        }
    }

    private suspend fun <T> io(block: suspend () -> T): Result<T> =
        withContext(ioDispatcher) {
            runCatchingResult(map = Throwable::toTrashError) { block() }
        }

    private companion object {
        const val CONTENT_SCHEME = "content://"
        const val TRASH_DIR_NAME = "trash"

        /** External-files trash (same volume as user files) with an internal fallback. */
        fun resolveTrashDir(context: Context): File =
            File(context.getExternalFilesDir(null) ?: context.filesDir, TRASH_DIR_NAME)
    }
}

private fun TrashEntity.toTrashedItem(): TrashedItem =
    TrashedItem(
        id = id,
        originalPath = originalPath,
        name = name,
        isDirectory = isDirectory,
        sizeBytes = sizeBytes,
        deletedAtEpochMillis = deletedAtEpochMillis,
    )

/** Total bytes of a file, or of every file under a directory tree. */
private fun File.sizeRecursive(): Long =
    if (isDirectory) walkTopDown().filter { it.isFile }.sumOf { it.length() } else length()

private fun Throwable.toTrashError(): OperationError =
    when (this) {
        is SecurityException -> OperationError.PermissionDenied(this)
        is IOException -> OperationError.Io(this)
        else -> OperationError.Unknown(this)
    }
