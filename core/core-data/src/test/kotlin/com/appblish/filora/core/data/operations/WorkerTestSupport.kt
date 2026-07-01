package com.appblish.filora.core.data.operations

import android.content.Context
import androidx.work.Data
import androidx.work.ProgressUpdater
import com.google.common.util.concurrent.ListenableFuture
import java.util.UUID
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

/**
 * A [ProgressUpdater] that records every progress [Data] a worker publishes through
 * `setProgress`, and lets a test react to each tick via [onTick] (used to flip a
 * cancellation signal mid-archive). Injected through
 * `TestListenableWorkerBuilder.setProgressUpdater(...)` — the default updater is a
 * no-op, so without this the worker's progress emissions are unobservable.
 */
internal class RecordingProgressUpdater(
    private val onTick: (Data) -> Unit = {},
) : ProgressUpdater {
    val updates = mutableListOf<Data>()

    override fun updateProgress(
        context: Context,
        id: UUID,
        data: Data,
    ): ListenableFuture<Void> {
        updates += data
        onTick(data)
        return ImmediateVoidFuture
    }
}

/**
 * An already-completed [ListenableFuture] returned by [RecordingProgressUpdater] so
 * `CoroutineWorker.setProgress` (which awaits the future) resumes immediately.
 * Implemented directly rather than via `concurrent-futures` to avoid depending on a
 * transitive artifact that is not part of `:core:core-data`'s declared classpath.
 */
internal object ImmediateVoidFuture : ListenableFuture<Void> {
    override fun addListener(
        listener: Runnable,
        executor: Executor,
    ) = executor.execute(listener)

    override fun cancel(mayInterruptIfRunning: Boolean): Boolean = false

    override fun isCancelled(): Boolean = false

    override fun isDone(): Boolean = true

    override fun get(): Void? = null

    override fun get(
        timeout: Long,
        unit: TimeUnit,
    ): Void? = null
}
