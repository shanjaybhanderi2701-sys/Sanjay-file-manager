package com.appblish.filora.core.domain.usecase

import com.appblish.filora.core.common.result.OperationError
import com.appblish.filora.core.common.result.Result
import com.appblish.filora.core.common.result.asError
import com.appblish.filora.core.common.result.asSuccess
import com.appblish.filora.core.domain.model.DeleteOutcome
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DeleteUseCaseTest {
    @Test
    fun `delegates de-duplicated non-blank paths to the repository`() =
        runTest {
            val outcome = DeleteOutcome(deletedCount = 2, movedToTrash = true)
            val repo = FakeFileRepository(deleteResult = outcome.asSuccess())
            val useCase = DeleteUseCase(repo)

            val result = useCase(listOf("/sd/a.txt", "/sd/a.txt", "  ", "/sd/docs"), toTrash = true)

            assertThat(result).isEqualTo(outcome.asSuccess())
            assertThat(repo.deleteArgs).isEqualTo(listOf("/sd/a.txt", "/sd/docs") to true)
        }

    @Test
    fun `forwards the permanent-delete flag`() =
        runTest {
            val repo = FakeFileRepository()
            val useCase = DeleteUseCase(repo)

            useCase(listOf("/sd/a.txt"), toTrash = false)

            assertThat(repo.deleteArgs).isEqualTo(listOf("/sd/a.txt") to false)
        }

    @Test
    fun `defaults to moving items to trash`() =
        runTest {
            val repo = FakeFileRepository()
            val useCase = DeleteUseCase(repo)

            useCase(listOf("/sd/a.txt"))

            assertThat(repo.deleteArgs?.second).isTrue()
        }

    @Test
    fun `an empty target list is rejected without touching the repository`() =
        runTest {
            val repo = FakeFileRepository()
            val useCase = DeleteUseCase(repo)

            val result = useCase(listOf("", "   "))

            assertThat(result).isEqualTo(OperationError.NotFound().asError())
            assertThat(repo.deleteArgs).isNull()
        }

    @Test
    fun `propagates a repository failure unchanged`() =
        runTest {
            val failure: Result<DeleteOutcome> = OperationError.PermissionDenied().asError()
            val repo = FakeFileRepository(deleteResult = failure)
            val useCase = DeleteUseCase(repo)

            val result = useCase(listOf("/sd/a.txt"))

            assertThat(result).isEqualTo(failure)
        }
}
