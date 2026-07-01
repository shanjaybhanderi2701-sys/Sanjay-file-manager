package com.appblish.filora.core.domain.repository

import com.appblish.filora.core.domain.model.ArchiveProgress
import com.appblish.filora.core.domain.model.ConflictStrategy
import com.appblish.filora.core.domain.model.FileOperationKind
import com.appblish.filora.core.domain.model.OperationProgress
import kotlinx.coroutines.flow.Flow

/**
 * Domain seam over the background file-operation scheduler (FR-3.5). Feature modules
 * see `core-domain` only — never `core-data` — so the copy/move/zip UI drives this
 * interface, and the data layer binds the concrete WorkManager-backed scheduler.
 *
 * Every call returns an *operation id* used to observe live progress with [progress]
 * / [compressProgress] and to [cancel] the work. Because WorkManager persists enqueued
 * work, an operation and its progress stream survive process death (NFR-2.3).
 */
interface FileOperationsScheduler {
    /**
     * Enqueues [kind] over [sources] into [destinationDir] (a local path or a SAF tree
     * URI; ignored for delete) and returns the operation id. [conflictStrategy] decides
     * how per-item name collisions are resolved (FR-3.3).
     */
    fun enqueue(
        kind: FileOperationKind,
        sources: List<String>,
        destinationDir: String? = null,
        toTrash: Boolean = true,
        conflictStrategy: ConflictStrategy = ConflictStrategy.KeepBoth,
    ): String

    /**
     * Enqueues a ZIP compression of [sources] into a new archive at the local
     * [destinationArchivePath] (FR-7.1) and returns the operation id.
     */
    fun enqueueCompress(
        sources: List<String>,
        destinationArchivePath: String,
    ): String

    /** Live progress for a copy/move/delete [operationId]; completes when the work terminates. */
    fun progress(operationId: String): Flow<OperationProgress>

    /** Live progress for a compress [operationId]; completes when the work terminates. */
    fun compressProgress(operationId: String): Flow<ArchiveProgress>

    /** Cancels the operation; the worker observes the stop and WorkManager records it cancelled. */
    fun cancel(operationId: String)
}
