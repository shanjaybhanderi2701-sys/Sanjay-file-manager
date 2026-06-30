package com.appblish.filora.core.data.operations

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.appblish.filora.core.domain.model.ArchiveProgress
import com.appblish.filora.core.domain.model.ConflictStrategy
import com.appblish.filora.core.domain.model.FileOperationKind
import com.appblish.filora.core.domain.model.OperationProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enqueues background file operations and exposes their live progress (FR-3.5).
 * This is the surface the browser ViewModel drives: it never touches WorkManager
 * or worker classes directly, only operation ids and [OperationProgress].
 *
 * Each call to [enqueue] returns a unique operation id used both as the
 * WorkManager unique-work name (so re-enqueuing the same id replaces it) and as
 * the [WorkRequestStore] key for large source lists.
 */
@Singleton
class OperationScheduler
    @Inject
    constructor(
        private val workManager: WorkManager,
        private val store: WorkRequestStore,
        private val idGenerator: OperationIdGenerator,
    ) {
        /**
         * Enqueues [kind] over [sources] into [destinationDir] (ignored for delete)
         * and returns the operation id to observe with [progress] / cancel with
         * [cancel]. Small lists travel inline in the work input; larger ones are
         * stashed in [store] and referenced by id (spec §2).
         */
        fun enqueue(
            kind: FileOperationKind,
            sources: List<String>,
            destinationDir: String? = null,
            toTrash: Boolean = true,
            conflictStrategy: ConflictStrategy = ConflictStrategy.KeepBoth,
        ): String {
            val operationId = idGenerator.newId()
            if (sources.size > OperationWorkData.INLINE_SOURCE_LIMIT) {
                store.put(operationId, sources)
            }
            val args = OperationArgs(
                kind = kind,
                sources = sources,
                destinationDir = destinationDir,
                toTrash = toTrash,
                sourcesRefKey = operationId.takeIf { sources.size > OperationWorkData.INLINE_SOURCE_LIMIT },
                conflictStrategy = conflictStrategy,
            )
            val request = buildRequest(kind, args)
            workManager.enqueueUniqueWork(operationId, ExistingWorkPolicy.REPLACE, request)
            return operationId
        }

        /**
         * Enqueues a background ZIP extraction of [archivePath] into
         * [destinationDir] with the given [conflictStrategy] (FR-7.2) and returns
         * the operation id to cancel with [cancel]. The extract worker is
         * self-contained, so no source list is stashed and nested-path/zip-slip
         * handling lives entirely in the worker's [ZipExtractor].
         */
        fun enqueueExtract(
            archivePath: String,
            destinationDir: String,
            conflictStrategy: ConflictStrategy = ConflictStrategy.KeepBoth,
        ): String {
            val operationId = idGenerator.newId()
            val input = ArchiveExtractWorkData.encodeInput(
                ArchiveExtractArgs(archivePath, destinationDir, conflictStrategy),
            )
            val request = OneTimeWorkRequestBuilder<ArchiveExtractWorker>()
            request
                .setInputData(input)
                .addTag(TAG_EXTRACT)
            workManager.enqueueUniqueWork(operationId, ExistingWorkPolicy.REPLACE, request.build())
            return operationId
        }

        /**
         * Enqueues a background ZIP compression of [sources] into a new archive at
         * [destinationArchivePath] (FR-7.1) and returns the operation id to observe
         * with [compressProgress] / cancel with [cancel]. The destination is the
         * SAF create-document location chosen by the UI; the compress worker is
         * self-contained, so tree-walk, entry naming and partial-output cleanup all
         * live in its [com.appblish.filora.core.data.archive.ZipCompressor].
         */
        fun enqueueCompress(
            sources: List<String>,
            destinationArchivePath: String,
        ): String {
            val operationId = idGenerator.newId()
            val input = ArchiveCompressWorkData.encodeInput(
                ArchiveCompressArgs(sources, destinationArchivePath),
            )
            val request = OneTimeWorkRequestBuilder<ArchiveCompressWorker>()
            request
                .setInputData(input)
                .addTag(TAG_COMPRESS)
            workManager.enqueueUniqueWork(operationId, ExistingWorkPolicy.REPLACE, request.build())
            return operationId
        }

        /** Live compression progress for [operationId]; completes once the work terminates. */
        fun compressProgress(operationId: String): Flow<ArchiveProgress> =
            workManager.getWorkInfosForUniqueWorkFlow(operationId).map { infos ->
                val info = infos.firstOrNull() ?: return@map ArchiveProgress.Pending
                toArchiveProgress(info)
            }

        /** Live progress for [operationId]; completes naturally once the work terminates. */
        fun progress(operationId: String): Flow<OperationProgress> =
            workManager.getWorkInfosForUniqueWorkFlow(operationId).map { infos ->
                val info = infos.firstOrNull() ?: return@map OperationProgress.Pending(FileOperationKind.Copy)
                toProgress(info)
            }

        /** Cancels the operation; the worker observes the stop and WorkManager records it cancelled. */
        fun cancel(operationId: String) {
            workManager.cancelUniqueWork(operationId)
        }

        private fun buildRequest(
            kind: FileOperationKind,
            args: OperationArgs
        ): OneTimeWorkRequest {
            val builder = when (kind) {
                FileOperationKind.Copy -> OneTimeWorkRequestBuilder<CopyWorker>()
                FileOperationKind.Move -> OneTimeWorkRequestBuilder<MoveWorker>()
                FileOperationKind.Delete -> OneTimeWorkRequestBuilder<DeleteWorker>()
            }
            return builder
                .setInputData(OperationWorkData.encodeInput(args))
                .addTag(kindTag(kind))
                .build()
        }

        private fun toProgress(info: WorkInfo): OperationProgress {
            val kind = kindFromTags(info.tags) ?: FileOperationKind.Copy
            return when (info.state) {
                WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> OperationProgress.Pending(kind)
                WorkInfo.State.RUNNING ->
                    OperationWorkData.decodeProgress(kind, info.progress) ?: OperationProgress.Pending(kind)
                WorkInfo.State.SUCCEEDED ->
                    OperationWorkData.decodeTerminal(kind, succeeded = true, cancelled = false, info.outputData)
                WorkInfo.State.CANCELLED ->
                    OperationWorkData.decodeTerminal(kind, succeeded = false, cancelled = true, info.outputData)
                WorkInfo.State.FAILED ->
                    OperationWorkData.decodeTerminal(kind, succeeded = false, cancelled = false, info.outputData)
            }
        }

        private fun toArchiveProgress(info: WorkInfo): ArchiveProgress =
            when (info.state) {
                WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> ArchiveProgress.Pending
                WorkInfo.State.RUNNING -> ArchiveCompressWorkData.decodeProgress(info.progress)
                WorkInfo.State.SUCCEEDED -> ArchiveCompressWorkData.decodeSuccess(info.outputData)
                WorkInfo.State.CANCELLED -> ArchiveProgress.Cancelled
                WorkInfo.State.FAILED ->
                    ArchiveProgress.Failed(ArchiveCompressWorkData.decodeError(info.outputData))
            }

        private companion object {
            const val TAG_PREFIX = "filora.op.kind:"
            const val TAG_EXTRACT = "filora.op.extract"
            const val TAG_COMPRESS = "filora.op.compress"

            fun kindTag(kind: FileOperationKind): String = TAG_PREFIX + kind.name

            fun kindFromTags(tags: Set<String>): FileOperationKind? =
                tags
                    .firstOrNull { it.startsWith(TAG_PREFIX) }
                    ?.removePrefix(TAG_PREFIX)
                    ?.let { runCatching { FileOperationKind.valueOf(it) }.getOrNull() }
        }
    }

/** Indirection over id creation so the scheduler stays unit-testable with a deterministic id. */
fun interface OperationIdGenerator {
    fun newId(): String
}
