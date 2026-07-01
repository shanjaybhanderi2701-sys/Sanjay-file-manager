package com.appblish.filora.core.domain.usecase

import com.appblish.filora.core.common.result.Result
import com.appblish.filora.core.common.result.asSuccess
import com.appblish.filora.core.domain.model.TrashRetention
import com.appblish.filora.core.domain.model.TrashedItem
import com.appblish.filora.core.domain.repository.TrashRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * In-memory [TrashRepository] for use-case tests. Records the arguments of the last
 * mutating call and replays scripted results, and mirrors the real impl's rule that
 * only non-`content://` paths are trashable (overridable via [trashablePredicate]).
 */
class FakeTrashRepository(
    private val items: List<TrashedItem> = emptyList(),
    private val totalSize: Long = 0L,
    private val moveResult: Result<Int> = 0.asSuccess(),
    private val restoreResult: Result<Int> = 0.asSuccess(),
    private val deleteForeverResult: Result<Int> = 0.asSuccess(),
    private val emptyResult: Result<Int> = 0.asSuccess(),
    private val purgeResult: Result<Int> = 0.asSuccess(),
    private val trashablePredicate: (String) -> Boolean = { !it.startsWith("content://") },
) : TrashRepository {
    var moveArgs: List<String>? = null
        private set
    var restoreArgs: List<String>? = null
        private set
    var deleteForeverArgs: List<String>? = null
        private set
    var emptyCalled = false
        private set
    var purgeArg: TrashRetention? = null
        private set

    override fun observeTrash(): Flow<List<TrashedItem>> = flowOf(items)

    override fun observeTrashSize(): Flow<Long> = flowOf(totalSize)

    override fun canTrash(path: String): Boolean = trashablePredicate(path)

    override suspend fun moveToTrash(paths: List<String>): Result<Int> {
        moveArgs = paths
        return moveResult
    }

    override suspend fun restore(ids: List<String>): Result<Int> {
        restoreArgs = ids
        return restoreResult
    }

    override suspend fun deleteForever(ids: List<String>): Result<Int> {
        deleteForeverArgs = ids
        return deleteForeverResult
    }

    override suspend fun emptyTrash(): Result<Int> {
        emptyCalled = true
        return emptyResult
    }

    override suspend fun purgeExpired(retention: TrashRetention): Result<Int> {
        purgeArg = retention
        return purgeResult
    }
}
