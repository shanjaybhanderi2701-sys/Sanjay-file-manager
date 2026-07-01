package com.appblish.filora.core.data.operations

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.appblish.filora.core.common.result.OperationError
import com.appblish.filora.core.domain.model.ConflictStrategy
import com.appblish.filora.core.domain.model.FileItem
import com.appblish.filora.core.domain.model.FileOperationKind
import com.appblish.filora.core.domain.model.OperationProgress
import com.appblish.filora.core.domain.model.TransferOutcome
import com.appblish.filora.core.domain.model.TransferResult
import com.appblish.filora.core.domain.repository.FileRepository
import com.appblish.filora.core.domain.repository.TrashRepository
import com.appblish.filora.core.domain.usecase.CopyUseCase
import com.appblish.filora.core.domain.usecase.DeleteUseCase
import com.appblish.filora.core.domain.usecase.MoveUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.Optional
import androidx.work.ListenableWorker.Result as WorkerResult
import com.appblish.filora.core.common.result.Result as OpResult

/**
 * Hilt entry point used by the workers to reach the application graph. Workers are
 * instantiated by WorkManager's default factory (no `androidx.hilt:hilt-work`
 * dependency), so dependencies are pulled here at `doWork` time rather than via
 * constructor injection. This keeps the worker layer self-contained in
 * `core-data` without touching the shared version catalogue.
 *
 * [fileRepository] is exposed as an [Optional] via `@BindsOptionalOf` (see
 * [FileOperationsBindings]) because the concrete `FileRepository` data binding is
 * contributed by the data-layer file-operations slice and is not wired yet. Until
 * it lands the optional resolves empty, so the `:app` Hilt graph still compiles
 * and a worker fails its task cleanly instead of the whole app failing on a
 * missing binding. The operation use cases are plain constructor-injected classes,
 * so the worker builds them directly from the repository rather than asking Hilt
 * for them (which would drag the missing `FileRepository` binding into the graph).
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface FileOperationWorkerEntryPoint {
    fun fileRepository(): Optional<FileRepository>

    fun trashRepository(): TrashRepository

    fun workRequestStore(): WorkRequestStore
}

/**
 * The application-graph dependencies a [FileOperationWorker] needs at run time.
 * Normally resolved from [FileOperationWorkerEntryPoint]; a test seam
 * ([FileOperationWorker.injectedDependencies]) can supply fakes directly so the
 * worker runs on the JVM without Hilt.
 */
internal data class OperationDependencies(
    val fileRepository: FileRepository?,
    val store: WorkRequestStore,
)

/** A copy/move batch call — both use cases share this shape, so the loop is kind-agnostic. */
private typealias TransferBatch = suspend (List<FileItem>, String, ConflictStrategy) -> OpResult<List<TransferResult>>

/**
 * Runs a long, large file operation in the background with a foreground
 * notification and live progress (FR-3.5). Because the work is enqueued through
 * WorkManager, it survives app backgrounding and process death — WorkManager
 * re-runs it on restart and the worker re-emits progress (NFR-2.3).
 *
 * The worker owns only the background mechanics — foreground promotion, per-item
 * progress, cancellation and the WorkManager result. The actual file work is
 * delegated to the domain use cases ([CopyUseCase]/[MoveUseCase]/[DeleteUseCase]),
 * so conflict handling and the copy-verify-delete safety ordering live in one
 * place. Copy/move are driven one source at a time: each call re-lists the
 * destination, so a keep-both name written for an earlier item is visible to the
 * next, and the notification advances per item.
 *
 * Concrete kinds are the thin [CopyWorker]/[MoveWorker]/[DeleteWorker] subclasses
 * so unique-work names and the spec's per-operation worker classes are preserved
 * while the loop lives in one place.
 */
internal abstract class FileOperationWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    private val entryPoint by lazy {
        EntryPointAccessors.fromApplication(
            applicationContext,
            FileOperationWorkerEntryPoint::class.java,
        )
    }
    private val notifier = OperationNotifier(appContext)

    /**
     * Test seam: when set, the worker draws its [FileRepository] + [WorkRequestStore]
     * from here instead of the Hilt [FileOperationWorkerEntryPoint]. Lets JVM/Robolectric
     * tests exercise [doWork] against a `FakeFileRepository` without standing up the
     * application Hilt graph (the production `FileRepository` binding is an empty
     * `@BindsOptionalOf` optional). `null` in production, where the entry point supplies
     * the real bindings.
     */
    @VisibleForTesting
    internal var injectedDependencies: OperationDependencies? = null

    private fun resolveDependencies(): OperationDependencies =
        injectedDependencies ?: OperationDependencies(
            fileRepository = entryPoint.fileRepository().orElse(null),
            store = entryPoint.workRequestStore(),
        )

    override suspend fun getForegroundInfo() = notifier.foregroundInfo(OperationProgress.Pending(kind))

    final override suspend fun doWork(): WorkerResult {
        val deps = resolveDependencies()
        val args = OperationWorkData.decodeInput(inputData) { deps.store.get(it) }
            ?: return success(0) // nothing to do (e.g. stashed list evicted after a cold restart)
        // The FileRepository data binding is not wired yet; fail cleanly rather than crash.
        val repository = deps.fileRepository ?: return failure(OperationError.Unknown())

        return try {
            setForeground(notifier.foregroundInfo(OperationProgress.Pending(kind)))
            when (args.kind) {
                FileOperationKind.Delete ->
                    runDelete(args, DeleteUseCase(repository, entryPoint.trashRepository()))
                FileOperationKind.Copy -> runTransfer(args, repository, CopyUseCase(repository)::invoke)
                FileOperationKind.Move ->
                    runTransfer(args, repository, MoveUseCase(CopyUseCase(repository), repository)::invoke)
            }
        } finally {
            // No catch here is intentional: a cooperative cancel (user pressed cancel /
            // WorkManager stopped us) must propagate as CancellationException so WorkManager
            // records the cancelled state instead of swallowing it as success. The cleanup
            // below still runs on both the success and cancellation paths.
            args.sourcesRefKey?.let { deps.store.remove(it) }
        }
    }

    private suspend fun runTransfer(
        args: OperationArgs,
        repository: FileRepository,
        transfer: TransferBatch,
    ): WorkerResult {
        val dest = args.destinationDir ?: return failure(OperationError.NotFound())
        val total = args.sources.size
        var processed = 0
        var firstError: OperationError? = null

        args.sources.forEachIndexed { index, source ->
            publish(
                OperationProgress.Running(
                    kind = kind,
                    itemIndex = index,
                    itemCount = total,
                    currentName = displayName(source),
                ),
            )
            val item = when (val resolved = repository.getFile(source)) {
                is OpResult.Success -> resolved.data
                is OpResult.Error -> {
                    firstError = firstError ?: resolved.error
                    return@forEachIndexed
                }
            }
            when (val outcome = transferItem(transfer, item, dest, args.conflictStrategy)) {
                is OpResult.Error -> firstError = firstError ?: outcome.error
                is OpResult.Success -> if (outcome.data) processed++
            }
        }

        return firstError?.let { failure(it) } ?: success(processed)
    }

    /** Transfers a single [item]; the boolean reports whether it was written (vs. intentionally skipped). */
    private suspend fun transferItem(
        transfer: TransferBatch,
        item: FileItem,
        dest: String,
        strategy: ConflictStrategy,
    ): OpResult<Boolean> =
        when (val batch = transfer(listOf(item), dest, strategy)) {
            is OpResult.Error -> OpResult.Error(batch.error)
            is OpResult.Success -> outcomeOf(batch.data)
        }

    private fun outcomeOf(results: List<TransferResult>): OpResult<Boolean> =
        when (val outcome = results.firstOrNull()?.outcome) {
            is TransferOutcome.Transferred -> OpResult.Success(true)
            is TransferOutcome.Skipped -> OpResult.Success(false)
            is TransferOutcome.Failed -> OpResult.Error(outcome.error)
            null -> OpResult.Success(false)
        }

    private suspend fun runDelete(
        args: OperationArgs,
        deleteUseCase: DeleteUseCase
    ): WorkerResult {
        publish(
            OperationProgress.Running(
                kind = kind,
                itemIndex = 0,
                itemCount = args.sources.size,
                currentName = args.sources
                    .firstOrNull()
                    ?.let(::displayName)
                    .orEmpty(),
            ),
        )
        return when (val result = deleteUseCase(args.sources, args.toTrash)) {
            is OpResult.Success -> success(result.data.deletedCount)
            is OpResult.Error -> failure(result.error)
        }
    }

    private suspend fun publish(progress: OperationProgress.Running) {
        setProgress(OperationWorkData.encodeProgress(progress))
        setForeground(notifier.foregroundInfo(progress))
    }

    private fun success(processed: Int): WorkerResult =
        WorkerResult.success(OperationWorkData.encodeSuccessOutput(processed))

    private fun failure(error: OperationError): WorkerResult =
        WorkerResult.failure(OperationWorkData.encodeFailureOutput(error))

    /** The operation this worker performs; fixes the notification copy and progress kind. */
    protected abstract val kind: FileOperationKind

    private fun displayName(path: String): String =
        path
            .trimEnd('/')
            .substringAfterLast('/')
            .substringAfterLast("%2F")
            .ifBlank { path }
}

/** Copies the selected sources into a destination directory in the background (FR-3.5). */
internal class CopyWorker(
    context: Context,
    params: WorkerParameters,
) : FileOperationWorker(context, params) {
    override val kind = FileOperationKind.Copy
}

/** Moves the selected sources into a destination directory in the background (FR-3.5). */
internal class MoveWorker(
    context: Context,
    params: WorkerParameters,
) : FileOperationWorker(context, params) {
    override val kind = FileOperationKind.Move
}

/** Deletes (or trashes) the selected sources in the background (FR-3.4, FR-3.5). */
internal class DeleteWorker(
    context: Context,
    params: WorkerParameters,
) : FileOperationWorker(context, params) {
    override val kind = FileOperationKind.Delete
}
