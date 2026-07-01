package com.appblish.filora.core.domain.usecase

import com.appblish.filora.core.common.result.OperationError
import com.appblish.filora.core.common.result.asError
import com.appblish.filora.core.common.result.asSuccess
import com.appblish.filora.core.domain.model.TrashRetention
import com.appblish.filora.core.domain.model.TrashedItem
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.Duration.Companion.days

/** Guards the recycle-bin use cases (T126/T127/T129/T128) over [FakeTrashRepository]. */
class TrashUseCasesTest {
    @Test
    fun `restore de-duplicates non-blank ids and delegates`() =
        runTest {
            val trash = FakeTrashRepository(restoreResult = 2.asSuccess())
            val result = RestoreFromTrashUseCase(trash)(listOf("a", "a", "  ", "b"))

            assertThat(result).isEqualTo(2.asSuccess())
            assertThat(trash.restoreArgs).isEqualTo(listOf("a", "b"))
        }

    @Test
    fun `restore rejects an empty request without touching the repository`() =
        runTest {
            val trash = FakeTrashRepository()
            val result = RestoreFromTrashUseCase(trash)(listOf("", "  "))

            assertThat(result).isEqualTo(OperationError.NotFound().asError())
            assertThat(trash.restoreArgs).isNull()
        }

    @Test
    fun `deleteForever de-duplicates and delegates`() =
        runTest {
            val trash = FakeTrashRepository(deleteForeverResult = 1.asSuccess())
            val result = DeleteForeverUseCase(trash)(listOf("x", "x"))

            assertThat(result).isEqualTo(1.asSuccess())
            assertThat(trash.deleteForeverArgs).isEqualTo(listOf("x"))
        }

    @Test
    fun `deleteForever rejects an empty request`() =
        runTest {
            val trash = FakeTrashRepository()
            val result = DeleteForeverUseCase(trash)(emptyList())

            assertThat(result).isEqualTo(OperationError.NotFound().asError())
            assertThat(trash.deleteForeverArgs).isNull()
        }

    @Test
    fun `emptyTrash delegates and returns the count`() =
        runTest {
            val trash = FakeTrashRepository(emptyResult = 3.asSuccess())
            val result = EmptyTrashUseCase(trash)()

            assertThat(result).isEqualTo(3.asSuccess())
            assertThat(trash.emptyCalled).isTrue()
        }

    @Test
    fun `purge forwards the configured retention`() =
        runTest {
            val trash = FakeTrashRepository(purgeResult = 4.asSuccess())
            val retention = TrashRetention(maxAge = 7.days)

            val result = PurgeExpiredTrashUseCase(trash)(retention)

            assertThat(result).isEqualTo(4.asSuccess())
            assertThat(trash.purgeArg).isEqualTo(retention)
        }

    @Test
    fun `observe use cases stream through from the repository`() =
        runTest {
            val item =
                TrashedItem(
                    id = "1",
                    originalPath = "/sd/a.txt",
                    name = "a.txt",
                    isDirectory = false,
                    sizeBytes = 42,
                    deletedAtEpochMillis = 100,
                )
            val trash = FakeTrashRepository(items = listOf(item), totalSize = 42)

            assertThat(ObserveTrashUseCase(trash)().first()).containsExactly(item)
            assertThat(ObserveTrashSizeUseCase(trash)().first()).isEqualTo(42L)
        }
}
