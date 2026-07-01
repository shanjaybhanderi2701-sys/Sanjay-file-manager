package com.appblish.filora.core.data.operations

import android.content.Context
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.appblish.filora.core.domain.model.ConflictStrategy
import com.appblish.filora.core.domain.model.FileOperationKind
import com.appblish.filora.core.testing.FakeFileRepository
import com.appblish.filora.core.testing.fileTree
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * JVM execution tests for the copy/move/delete workers ([CopyWorker], [MoveWorker],
 * [DeleteWorker]) that share [FileOperationWorker] (T167). Covers the worker
 * orchestration — input decode, dispatch to the domain use cases, conflict-strategy
 * pass-through, progress emission and the WorkManager result — on Robolectric with no
 * emulator.
 *
 * The Hilt `@EntryPoint` is bypassed via the [FileOperationWorker.injectedDependencies]
 * seam, wiring an in-memory [FakeFileRepository] (from `:core:core-testing`) directly.
 * The copy/move/delete conflict *semantics* themselves are covered exhaustively by the
 * domain `CopyUseCaseTest`/`MoveUseCaseTest`/`DeleteUseCaseTest`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class FileOperationWorkerTest {
    private val context: Context get() = RuntimeEnvironment.getApplication()

    private inline fun <reified W : FileOperationWorker> buildWorker(
        repository: FakeFileRepository,
        kind: FileOperationKind,
        sources: List<String>,
        destinationDir: String? = null,
        strategy: ConflictStrategy = ConflictStrategy.KeepBoth,
        toTrash: Boolean = true,
        progressUpdater: RecordingProgressUpdater = RecordingProgressUpdater(),
    ): W {
        val input: Data = OperationWorkData.encodeInput(
            OperationArgs(
                kind = kind,
                sources = sources,
                destinationDir = destinationDir,
                toTrash = toTrash,
                sourcesRefKey = null,
                conflictStrategy = strategy,
            ),
        )
        return TestListenableWorkerBuilder
            .from(context, W::class.java)
            .setInputData(input)
            .setProgressUpdater(progressUpdater)
            .build()
            .apply { injectedDependencies = OperationDependencies(repository, WorkRequestStore()) }
    }

    @Test
    fun `copy writes the source into the destination and succeeds`() {
        val repo = FakeFileRepository(
            fileTree("/vol") {
                dir("src") { file("a.txt", sizeBytes = 4) }
                dir("dst") {}
            },
        )
        val worker = buildWorker<CopyWorker>(
            repo,
            FileOperationKind.Copy,
            sources = listOf("/vol/src/a.txt"),
            destinationDir = "/vol/dst",
        )

        val result = runBlocking { worker.doWork() }

        assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
        assertThat(repo.items).containsKey("/vol/dst/a.txt")
        assertThat(repo.items).containsKey("/vol/src/a.txt") // copy leaves the source in place
    }

    @Test
    fun `copy with skip strategy leaves the colliding destination untouched`() {
        val repo = FakeFileRepository(
            fileTree("/vol") {
                dir("src") { file("a.txt", sizeBytes = 4) }
                dir("dst") { file("a.txt", sizeBytes = 9) }
            },
        )
        val worker = buildWorker<CopyWorker>(
            repo,
            FileOperationKind.Copy,
            sources = listOf("/vol/src/a.txt"),
            destinationDir = "/vol/dst",
            strategy = ConflictStrategy.Skip,
        )

        val result = runBlocking { worker.doWork() }

        assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
        assertThat(repo.copyCalls).isEmpty()
        assertThat(repo.items.getValue("/vol/dst/a.txt").sizeBytes).isEqualTo(9)
    }

    @Test
    fun `copy with replace strategy overwrites the colliding destination`() {
        val repo = FakeFileRepository(
            fileTree("/vol") {
                dir("src") { file("a.txt", sizeBytes = 4) }
                dir("dst") { file("a.txt", sizeBytes = 9) }
            },
        )
        val worker = buildWorker<CopyWorker>(
            repo,
            FileOperationKind.Copy,
            sources = listOf("/vol/src/a.txt"),
            destinationDir = "/vol/dst",
            strategy = ConflictStrategy.Replace,
        )

        runBlocking { worker.doWork() }

        assertThat(repo.items.getValue("/vol/dst/a.txt").sizeBytes).isEqualTo(4)
    }

    @Test
    fun `copy with keep-both strategy writes the source under a fresh name`() {
        val repo = FakeFileRepository(
            fileTree("/vol") {
                dir("src") { file("a.txt", sizeBytes = 4) }
                dir("dst") { file("a.txt", sizeBytes = 9) }
            },
        )
        val worker = buildWorker<CopyWorker>(
            repo,
            FileOperationKind.Copy,
            sources = listOf("/vol/src/a.txt"),
            destinationDir = "/vol/dst",
            strategy = ConflictStrategy.KeepBoth,
        )

        runBlocking { worker.doWork() }

        assertThat(repo.items).containsKey("/vol/dst/a (1).txt")
        assertThat(repo.items.getValue("/vol/dst/a.txt").sizeBytes).isEqualTo(9)
    }

    @Test
    fun `copy publishes per-item progress`() {
        val repo = FakeFileRepository(
            fileTree("/vol") {
                dir("src") { file("a.txt", sizeBytes = 4) }
                dir("dst") {}
            },
        )
        val recorder = RecordingProgressUpdater()
        val worker = buildWorker<CopyWorker>(
            repo,
            FileOperationKind.Copy,
            sources = listOf("/vol/src/a.txt"),
            destinationDir = "/vol/dst",
            progressUpdater = recorder,
        )

        runBlocking { worker.doWork() }

        assertThat(recorder.updates).isNotEmpty()
        val progress = OperationWorkData.decodeProgress(FileOperationKind.Copy, recorder.updates.last())
        assertThat(progress?.itemCount).isEqualTo(1)
    }

    @Test
    fun `move copies then removes the source and succeeds`() {
        val repo = FakeFileRepository(
            fileTree("/vol") {
                dir("src") { file("a.txt", sizeBytes = 4) }
                dir("dst") {}
            },
        )
        val worker = buildWorker<MoveWorker>(
            repo,
            FileOperationKind.Move,
            sources = listOf("/vol/src/a.txt"),
            destinationDir = "/vol/dst",
        )

        val result = runBlocking { worker.doWork() }

        assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
        assertThat(repo.items).containsKey("/vol/dst/a.txt")
        assertThat(repo.items).doesNotContainKey("/vol/src/a.txt")
    }

    @Test
    fun `delete removes every source and succeeds`() {
        val repo = FakeFileRepository(
            fileTree("/vol") {
                file("a.txt", sizeBytes = 4)
                file("b.txt", sizeBytes = 8)
            },
        )
        val worker = buildWorker<DeleteWorker>(
            repo,
            FileOperationKind.Delete,
            sources = listOf("/vol/a.txt", "/vol/b.txt"),
        )

        val result = runBlocking { worker.doWork() }

        assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
        assertThat(repo.items).doesNotContainKey("/vol/a.txt")
        assertThat(repo.items).doesNotContainKey("/vol/b.txt")
    }

    @Test
    fun `missing repository binding fails the worker cleanly`() {
        val worker = buildWorker<DeleteWorker>(
            FakeFileRepository(fileTree("/vol") { file("a.txt", sizeBytes = 4) }),
            FileOperationKind.Delete,
            sources = listOf("/vol/a.txt"),
        )
        // The FileRepository binding is an empty @BindsOptionalOf optional until the data
        // slice lands; the worker must fail cleanly rather than crash when it resolves null.
        worker.injectedDependencies = OperationDependencies(fileRepository = null, store = WorkRequestStore())

        val result = runBlocking { worker.doWork() }

        assertThat(result).isInstanceOf(ListenableWorker.Result.Failure::class.java)
    }
}
