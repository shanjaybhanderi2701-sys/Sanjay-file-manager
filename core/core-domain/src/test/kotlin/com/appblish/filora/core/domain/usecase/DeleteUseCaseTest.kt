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
    fun `de-duplicated trashable paths are moved to trash`() =
        runTest {
            val file = FakeFileRepository()
            val trash = FakeTrashRepository(moveResult = 2.asSuccess())
            val useCase = DeleteUseCase(file, trash)

            val result = useCase(listOf("/sd/a.txt", "/sd/a.txt", "  ", "/sd/docs"), toTrash = true)

            assertThat(result).isEqualTo(DeleteOutcome(deletedCount = 2, movedToTrash = true).asSuccess())
            assertThat(trash.moveArgs).isEqualTo(listOf("/sd/a.txt", "/sd/docs"))
            // Trash was used, so the file repository's permanent delete was untouched.
            assertThat(file.deleteArgs).isNull()
        }

    @Test
    fun `permanent-delete request bypasses trash and removes via file repository`() =
        runTest {
            val outcome = DeleteOutcome(deletedCount = 1, movedToTrash = false)
            val file = FakeFileRepository(deleteResult = outcome.asSuccess())
            val trash = FakeTrashRepository()
            val useCase = DeleteUseCase(file, trash)

            val result = useCase(listOf("/sd/a.txt"), toTrash = false)

            assertThat(result).isEqualTo(outcome.asSuccess())
            assertThat(file.deleteArgs).isEqualTo(listOf("/sd/a.txt") to false)
            assertThat(trash.moveArgs).isNull()
        }

    @Test
    fun `a non-trashable target forces the whole batch to permanent delete`() =
        runTest {
            val outcome = DeleteOutcome(deletedCount = 2, movedToTrash = false)
            val file = FakeFileRepository(deleteResult = outcome.asSuccess())
            val trash = FakeTrashRepository()
            val useCase = DeleteUseCase(file, trash)

            // content:// is not trashable → even with toTrash=true the batch is permanent.
            val result =
                useCase(listOf("/sd/a.txt", "content://tree/doc"), toTrash = true)

            assertThat(result).isEqualTo(outcome.asSuccess())
            assertThat(file.deleteArgs).isEqualTo(listOf("/sd/a.txt", "content://tree/doc") to false)
            assertThat(trash.moveArgs).isNull()
        }

    @Test
    fun `defaults to moving items to trash`() =
        runTest {
            val trash = FakeTrashRepository(moveResult = 1.asSuccess())
            val useCase = DeleteUseCase(FakeFileRepository(), trash)

            useCase(listOf("/sd/a.txt"))

            assertThat(trash.moveArgs).isEqualTo(listOf("/sd/a.txt"))
        }

    @Test
    fun `an empty target list is rejected without touching either repository`() =
        runTest {
            val file = FakeFileRepository()
            val trash = FakeTrashRepository()
            val useCase = DeleteUseCase(file, trash)

            val result = useCase(listOf("", "   "))

            assertThat(result).isEqualTo(OperationError.NotFound().asError())
            assertThat(file.deleteArgs).isNull()
            assertThat(trash.moveArgs).isNull()
        }

    @Test
    fun `propagates a trash failure unchanged`() =
        runTest {
            val failure: Result<Int> = OperationError.Io().asError()
            val trash = FakeTrashRepository(moveResult = failure)
            val useCase = DeleteUseCase(FakeFileRepository(), trash)

            val result = useCase(listOf("/sd/a.txt"))

            assertThat(result).isEqualTo(OperationError.Io().asError())
        }
}
