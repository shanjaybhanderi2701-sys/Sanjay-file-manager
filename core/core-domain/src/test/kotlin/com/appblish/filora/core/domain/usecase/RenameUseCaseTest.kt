package com.appblish.filora.core.domain.usecase

import com.appblish.filora.core.common.result.OperationError
import com.appblish.filora.core.common.result.asError
import com.appblish.filora.core.common.result.asSuccess
import com.appblish.filora.core.domain.usecase.FakeFileRepository.Companion.sampleItem
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RenameUseCaseTest {
    @Test
    fun `delegates a valid trimmed name to the repository`() =
        runTest {
            val renamed = sampleItem("Invoices")
            val repo = FakeFileRepository(renameResult = renamed.asSuccess())
            val useCase = RenameUseCase(repo)

            val result = useCase("/root/Reports", "  Invoices  ", currentName = "Reports")

            assertThat(result).isEqualTo(renamed.asSuccess())
            assertThat(repo.renameArgs).isEqualTo("/root/Reports" to "Invoices")
        }

    @Test
    fun `rejects invalid characters without touching the repository`() =
        runTest {
            val repo = FakeFileRepository()
            val useCase = RenameUseCase(repo)

            val result = useCase("/root/Reports", "a*b?", currentName = "Reports")

            assertThat(result).isEqualTo(OperationError.InvalidName().asError())
            assertThat(repo.renameArgs).isNull()
        }

    @Test
    fun `unchanged name short-circuits to getFile rather than a rename`() =
        runTest {
            val current = sampleItem("Reports")
            val repo = FakeFileRepository(getFileResult = current.asSuccess())
            val useCase = RenameUseCase(repo)

            val result = useCase("/root/Reports", "Reports", currentName = "Reports")

            assertThat(result).isEqualTo(current.asSuccess())
            assertThat(repo.renameArgs).isNull()
            assertThat(repo.getFileArg).isEqualTo("/root/Reports")
        }
}
