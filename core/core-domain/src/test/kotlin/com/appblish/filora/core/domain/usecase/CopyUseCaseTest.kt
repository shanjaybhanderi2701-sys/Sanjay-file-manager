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

class CopyUseCaseTest {
    @Test
    fun `copies into an empty directory under the original name without overwrite`() =
        runTest {
            val repo = FakeFileRepository(listing = emptyList<FileItem>().asSuccess())
            val source = sampleItem("report.pdf", isDirectory = false)

            val result = CopyUseCase(repo)(listOf(source), DEST, ConflictStrategy.KeepBoth)

            val outcomes = (result as Result.Success).data
            assertThat(outcomes.single().outcome).isInstanceOf(TransferOutcome.Transferred::class.java)
            assertThat(repo.copyCalls.single())
                .isEqualTo(FakeFileRepository.CopyCall(source.path, DEST, "report.pdf", overwrite = false))
        }

    @Test
    fun `returns error and copies nothing when the destination cannot be listed`() =
        runTest {
            val repo = FakeFileRepository(listing = OperationError.PermissionDenied().asError())

            val result = CopyUseCase(repo)(listOf(sampleItem("a.txt")), DEST, ConflictStrategy.Replace)

            assertThat(result).isEqualTo(OperationError.PermissionDenied().asError())
            assertThat(repo.copyCalls).isEmpty()
        }

    @Test
    fun `skip strategy leaves a colliding source untransferred`() =
        runTest {
            val repo = FakeFileRepository(listing = listOf(sampleItem("a.txt")).asSuccess())

            val result = CopyUseCase(repo)(listOf(sampleItem("a.txt")), DEST, ConflictStrategy.Skip)

            val outcomes = (result as Result.Success).data
            assertThat(outcomes.single().outcome).isEqualTo(TransferOutcome.Skipped)
            assertThat(repo.copyCalls).isEmpty()
        }

    @Test
    fun `replace strategy copies a colliding source with overwrite`() =
        runTest {
            val repo = FakeFileRepository(listing = listOf(sampleItem("a.txt")).asSuccess())

            CopyUseCase(repo)(listOf(sampleItem("a.txt")), DEST, ConflictStrategy.Replace)

            assertThat(repo.copyCalls.single().overwrite).isTrue()
            assertThat(repo.copyCalls.single().destinationName).isEqualTo("a.txt")
        }

    @Test
    fun `keep-both copies a colliding source under a generated name`() =
        runTest {
            val repo = FakeFileRepository(listing = listOf(sampleItem("a.txt", isDirectory = false)).asSuccess())

            CopyUseCase(repo)(listOf(sampleItem("a.txt", isDirectory = false)), DEST, ConflictStrategy.KeepBoth)

            assertThat(repo.copyCalls.single().destinationName).isEqualTo("a (1).txt")
            assertThat(repo.copyCalls.single().overwrite).isFalse()
        }

    @Test
    fun `keep-both reserves generated names across the batch`() =
        runTest {
            val repo = FakeFileRepository(listing = listOf(sampleItem("a.txt", isDirectory = false)).asSuccess())
            val sources = listOf(sampleItem("a.txt", isDirectory = false), sampleItem("a.txt", isDirectory = false))

            CopyUseCase(repo)(sources, DEST, ConflictStrategy.KeepBoth)

            assertThat(repo.copyCalls.map { it.destinationName }).containsExactly("a (1).txt", "a (2).txt").inOrder()
        }

    @Test
    fun `a failed copy is reported per item without aborting the batch`() =
        runTest {
            val failing = sampleItem("bad.txt", isDirectory = false)
            val ok = sampleItem("good.txt", isDirectory = false)
            val repo = FakeFileRepository(copyFailurePaths = setOf(failing.path))

            val result = CopyUseCase(repo)(listOf(failing, ok), DEST, ConflictStrategy.Replace)

            val outcomes = (result as Result.Success).data
            assertThat(outcomes[0].outcome).isEqualTo(TransferOutcome.Failed(OperationError.Io()))
            assertThat(outcomes[1].outcome).isInstanceOf(TransferOutcome.Transferred::class.java)
        }
}
