package com.appblish.filora.core.domain.usecase

import com.appblish.filora.core.common.result.OperationError
import com.appblish.filora.core.common.result.Result
import com.appblish.filora.core.common.result.asError
import com.appblish.filora.core.common.result.asSuccess
import com.appblish.filora.core.domain.usecase.FakeFileRepository.Companion.sampleItem
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class CreateFolderUseCaseTest {
    @Test
    fun `delegates a valid trimmed name to the repository`() =
        runTest {
            val created = sampleItem("Reports")
            val repo = FakeFileRepository(createResult = created.asSuccess())
            val useCase = CreateFolderUseCase(repo)

            val result = useCase("/root", "  Reports  ")

            assertThat(result).isEqualTo(created.asSuccess())
            assertThat(repo.createFolderArgs).isEqualTo("/root" to "Reports")
        }

    @Test
    fun `rejects an invalid name without touching the repository`() =
        runTest {
            val repo = FakeFileRepository()
            val useCase = CreateFolderUseCase(repo)

            val result = useCase("/root", "bad/name")

            assertThat(result).isEqualTo(OperationError.InvalidName().asError())
            assertThat(repo.createFolderArgs).isNull()
        }

    @Test
    fun `propagates a conflict from the repository for a duplicate name`() =
        runTest {
            val repo = FakeFileRepository(createResult = OperationError.Conflict(path = "/root/Reports").asError())
            val useCase = CreateFolderUseCase(repo)

            val result = useCase("/root", "Reports")

            assertThat(result).isInstanceOf(Result.Error::class.java)
            assertThat((result as Result.Error).error).isInstanceOf(OperationError.Conflict::class.java)
        }
}
