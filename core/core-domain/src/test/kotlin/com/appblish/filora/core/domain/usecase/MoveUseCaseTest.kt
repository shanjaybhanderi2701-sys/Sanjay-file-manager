package com.appblish.filora.core.domain.usecase

import com.appblish.filora.core.common.result.OperationError
import com.appblish.filora.core.common.result.Result
import com.appblish.filora.core.common.result.asError
import com.appblish.filora.core.common.result.asSuccess
import com.appblish.filora.core.domain.model.ConflictStrategy
import com.appblish.filora.core.domain.model.FileItem
import com.appblish.filora.core.domain.model.TransferOutcome
import com.appblish.filora.core.domain.usecase.FakeFileRepository.Companion.sampleItem
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

private const val DEST = "/storage/emulated/0/Dest"

/** A destination entry the fake's `getFile` returns to verify a copy landed. */
private fun fileAt(
    path: String,
    sizeBytes: Long,
    isDirectory: Boolean = false,
): Result<FileItem> =
    FileItem(
        name = path.substringAfterLast('/'),
        path = path,
        isDirectory = isDirectory,
        sizeBytes = sizeBytes,
        lastModifiedEpochMillis = 0L,
    ).asSuccess()

class MoveUseCaseTest {
    private fun moveUseCase(repo: FakeFileRepository) = MoveUseCase(CopyUseCase(repo), repo)

    @Test
    fun `verified copy deletes the source permanently`() =
        runTest {
            val source = sampleItem("report.pdf", isDirectory = false, sizeBytes = 100L)
            val repo =
                FakeFileRepository(
                    listing = emptyList<FileItem>().asSuccess(),
                    getFileByPath = { path -> fileAt(path, sizeBytes = 100L) },
                )

            val result = moveUseCase(repo)(listOf(source), DEST, ConflictStrategy.Replace)

            val outcome = (result as Result.Success).data.single().outcome
            assertThat(outcome).isInstanceOf(TransferOutcome.Transferred::class.java)
            assertThat(repo.deleteArgs).isEqualTo(listOf(source.path) to false)
        }

    @Test
    fun `a directory is verified by existence and then deleted`() =
        runTest {
            val source = sampleItem("Album", isDirectory = true)
            val repo =
                FakeFileRepository(
                    listing = emptyList<FileItem>().asSuccess(),
                    getFileByPath = { path -> fileAt(path, sizeBytes = 999L, isDirectory = true) },
                )

            moveUseCase(repo)(listOf(source), DEST, ConflictStrategy.Replace)

            assertThat(repo.deleteArgs).isEqualTo(listOf(source.path) to false)
        }

    @Test
    fun `a copy that cannot be verified is not deleted`() =
        runTest {
            val source = sampleItem("report.pdf", isDirectory = false, sizeBytes = 100L)
            val repo =
                FakeFileRepository(
                    listing = emptyList<FileItem>().asSuccess(),
                    getFileByPath = { OperationError.NotFound().asError() },
                )

            val result = moveUseCase(repo)(listOf(source), DEST, ConflictStrategy.Replace)

            assertThat((result as Result.Success).data.single().outcome)
                .isEqualTo(TransferOutcome.Failed(OperationError.Io()))
            assertThat(repo.deleteArgs).isNull()
        }

    @Test
    fun `a size mismatch fails verification and keeps the source`() =
        runTest {
            val source = sampleItem("report.pdf", isDirectory = false, sizeBytes = 100L)
            val repo =
                FakeFileRepository(
                    listing = emptyList<FileItem>().asSuccess(),
                    getFileByPath = { path -> fileAt(path, sizeBytes = 99L) },
                )

            val result = moveUseCase(repo)(listOf(source), DEST, ConflictStrategy.Replace)

            assertThat((result as Result.Success).data.single().outcome)
                .isEqualTo(TransferOutcome.Failed(OperationError.Io()))
            assertThat(repo.deleteArgs).isNull()
        }

    @Test
    fun `a failed delete surfaces as a per-item failure`() =
        runTest {
            val source = sampleItem("report.pdf", isDirectory = false, sizeBytes = 100L)
            val repo =
                FakeFileRepository(
                    listing = emptyList<FileItem>().asSuccess(),
                    getFileByPath = { path -> fileAt(path, sizeBytes = 100L) },
                    deleteResult = OperationError.PermissionDenied().asError(),
                )

            val result = moveUseCase(repo)(listOf(source), DEST, ConflictStrategy.Replace)

            assertThat((result as Result.Success).data.single().outcome)
                .isEqualTo(TransferOutcome.Failed(OperationError.PermissionDenied()))
        }

    @Test
    fun `a skipped collision leaves the source in place`() =
        runTest {
            val source = sampleItem("a.txt", isDirectory = false)
            val repo = FakeFileRepository(listing = listOf(sampleItem("a.txt")).asSuccess())

            val result = moveUseCase(repo)(listOf(source), DEST, ConflictStrategy.Skip)

            assertThat((result as Result.Success).data.single().outcome).isEqualTo(TransferOutcome.Skipped)
            assertThat(repo.deleteArgs).isNull()
        }

    @Test
    fun `a failed copy is never deleted`() =
        runTest {
            val source = sampleItem("report.pdf", isDirectory = false, sizeBytes = 100L)
            val repo =
                FakeFileRepository(
                    listing = emptyList<FileItem>().asSuccess(),
                    copyFailurePaths = setOf(source.path),
                )

            val result = moveUseCase(repo)(listOf(source), DEST, ConflictStrategy.Replace)

            assertThat((result as Result.Success).data.single().outcome)
                .isEqualTo(TransferOutcome.Failed(OperationError.Io()))
            assertThat(repo.deleteArgs).isNull()
        }

    @Test
    fun `an unreadable destination aborts the move`() =
        runTest {
            val repo = FakeFileRepository(listing = OperationError.PermissionDenied().asError())

            val result = moveUseCase(repo)(listOf(sampleItem("a.txt")), DEST, ConflictStrategy.Replace)

            assertThat(result).isEqualTo(OperationError.PermissionDenied().asError())
            assertThat(repo.deleteArgs).isNull()
        }
}
