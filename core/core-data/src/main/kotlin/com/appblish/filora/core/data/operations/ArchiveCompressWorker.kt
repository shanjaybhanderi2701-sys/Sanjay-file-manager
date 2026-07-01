package com.appblish.filora.core.data.operations

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.appblish.filora.core.common.result.OperationError
import com.appblish.filora.core.data.archive.ZipCompressor
import com.appblish.filora.core.domain.model.ArchiveProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import androidx.work.ListenableWorker.Result as WorkerResult
import com.appblish.filora.core.common.result.Result as OpResult

/**
 * Compresses one or more files/folders into a ZIP in the background, surviving app
 * backgrounding and process death via WorkManager (FR-7.1, NFR-2.3, depends on the
 * T3.5 worker infrastructure). The worker owns only the background mechanics —
 * foreground promotion, progress publication, cooperative cancellation and the
 * WorkManager result; the actual archiving (tree walk, entry naming, partial-output
 * cleanup) is delegated to the pure [ZipCompressor], which has no Android
 * dependencies and is exhaustively unit-tested.
 *
 * Like the extract worker it needs no application-graph dependency (the compressor
 * works directly on file paths), so it is instantiated by WorkManager's default
 * factory with no Hilt entry point. The destination path is chosen by the UI via
 * SAF create-document (FR-7.1) and passed in as an opaque locator string.
 *
 * Progress is determinate: the compressor reports each file as it is written, which
 * the worker forwards through [setProgress] (observed by the scheduler as
 * [ArchiveProgress]) and the foreground notification. A user/WorkManager stop is
 * observed through [isStopped]; the compressor aborts mid-archive and deletes the
 * half-written output, so a cancel never leaves a corrupt ZIP behind.
 */
internal class ArchiveCompressWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    private val notifier = ArchiveCompressNotifier(appContext)

    /**
     * Test seam for cooperative cancellation. Production leaves this `null` and the
     * worker observes WorkManager's own [isStopped]; a JVM/Robolectric test supplies a
     * signal that flips mid-archive to drive the cancel-cleans-partial path
     * deterministically (WorkManager has no supported way to stop a
     * `TestListenableWorkerBuilder` worker).
     */
    @VisibleForTesting
    internal var stoppedSignalOverride: (() -> Boolean)? = null

    private fun stopped(): Boolean = stoppedSignalOverride?.invoke() ?: isStopped

    override suspend fun getForegroundInfo() = notifier.foregroundInfo()

    override suspend fun doWork(): WorkerResult {
        val args = ArchiveCompressWorkData.decodeInput(inputData)
            ?: return WorkerResult.failure(ArchiveCompressWorkData.encodeFailure(OperationError.InvalidName()))

        setForeground(notifier.foregroundInfo())

        val updates = Channel<ArchiveProgress.Running>(Channel.CONFLATED)
        return coroutineScope {
            val pump = launch { drainProgress(updates) }
            val outcome = withContext(Dispatchers.IO) {
                ZipCompressor().compress(
                    sources = args.sources.map(::File),
                    destination = File(args.destinationArchivePath),
                    onProgress = { index, total, name ->
                        updates.trySend(ArchiveProgress.Running(index, total, name))
                    },
                    isActive = { !stopped() },
                )
            }
            updates.close()
            pump.join()
            when (outcome) {
                is OpResult.Success ->
                    WorkerResult.success(
                        ArchiveCompressWorkData.encodeSuccess(args.destinationArchivePath, outcome.data),
                    )
                is OpResult.Error ->
                    WorkerResult.failure(ArchiveCompressWorkData.encodeFailure(outcome.error))
            }
        }
    }

    /** Forwards each progress tick to WorkManager state and the foreground notification. */
    private suspend fun drainProgress(updates: Channel<ArchiveProgress.Running>) {
        for (running in updates) {
            setProgress(ArchiveCompressWorkData.encodeProgress(running))
            // A stop can race a foreground refresh; ignore the resulting failure.
            runCatching {
                setForeground(notifier.foregroundInfo(running.processedEntries, running.totalEntries))
            }
        }
    }
}
