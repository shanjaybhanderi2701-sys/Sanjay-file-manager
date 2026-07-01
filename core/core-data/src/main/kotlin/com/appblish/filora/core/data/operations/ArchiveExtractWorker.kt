package com.appblish.filora.core.data.operations

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.appblish.filora.core.common.result.OperationError
import com.appblish.filora.core.data.archive.ZipExtractor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import androidx.work.ListenableWorker.Result as WorkerResult
import com.appblish.filora.core.common.result.Result as OpResult

/**
 * Extracts a ZIP archive in the background, surviving app backgrounding and process
 * death via WorkManager (FR-7.2, depends on the T3.5 worker infrastructure). The
 * worker owns only the background mechanics — foreground promotion, cooperative
 * cancellation and the WorkManager result; the actual extraction (nested-path
 * reconstruction, conflict handling and zip-slip safety) is delegated to the pure
 * [ZipExtractor], which has no Android dependencies and is exhaustively unit-tested.
 *
 * Unlike the copy/move/delete workers this one needs no application-graph
 * dependency (the extractor works directly on file paths), so it is instantiated by
 * WorkManager's default factory with no Hilt entry point. A user/WorkManager stop
 * is observed through [isStopped], which the extractor polls per entry and aborts
 * with a [CancellationException] rather than reporting a partial success.
 */
internal class ArchiveExtractWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    private val notifier = ArchiveExtractNotifier(appContext)

    /**
     * Test seam for cooperative cancellation — see [ArchiveCompressWorker.stoppedSignalOverride].
     * `null` in production, where the extractor polls WorkManager's [isStopped].
     */
    @VisibleForTesting
    internal var stoppedSignalOverride: (() -> Boolean)? = null

    private fun stopped(): Boolean = stoppedSignalOverride?.invoke() ?: isStopped

    override suspend fun getForegroundInfo() = notifier.foregroundInfo()

    override suspend fun doWork(): WorkerResult {
        val args = ArchiveExtractWorkData.decodeInput(inputData)
            ?: return WorkerResult.failure(ArchiveExtractWorkData.encodeFailure(OperationError.NotFound()))

        setForeground(notifier.foregroundInfo())

        return withContext(Dispatchers.IO) {
            val outcome = ZipExtractor().extract(
                archive = File(args.archivePath),
                destinationDir = File(args.destinationDir),
                strategy = args.strategy,
                isActive = { !stopped() },
            )
            when (outcome) {
                is OpResult.Success -> WorkerResult.success(ArchiveExtractWorkData.encodeSuccess(outcome.data))
                is OpResult.Error -> WorkerResult.failure(ArchiveExtractWorkData.encodeFailure(outcome.error))
            }
        }
    }
}
