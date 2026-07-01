package com.appblish.filora.feature.storage

import app.cash.turbine.test
import com.appblish.filora.core.common.result.OperationError
import com.appblish.filora.core.common.result.Result
import com.appblish.filora.core.common.result.asError
import com.appblish.filora.core.common.result.asSuccess
import com.appblish.filora.core.domain.model.TrashRetention
import com.appblish.filora.core.domain.model.TrashedItem
import com.appblish.filora.core.domain.repository.TrashRepository
import com.appblish.filora.core.domain.usecase.DeleteForeverUseCase
import com.appblish.filora.core.domain.usecase.EmptyTrashUseCase
import com.appblish.filora.core.domain.usecase.ObserveTrashSizeUseCase
import com.appblish.filora.core.domain.usecase.ObserveTrashUseCase
import com.appblish.filora.core.domain.usecase.PurgeExpiredTrashUseCase
import com.appblish.filora.core.domain.usecase.RestoreFromTrashUseCase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecycleBinViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** In-memory recycle bin whose reactive state updates on each mutating call. */
    private class FakeTrash(
        initial: List<TrashedItem> = emptyList(),
    ) : TrashRepository {
        val rows = MutableStateFlow(initial.associateBy { it.id })
        var purgeCalls = 0

        override fun observeTrash(): Flow<List<TrashedItem>> =
            rows.map { it.values.sortedByDescending(TrashedItem::deletedAtEpochMillis) }

        override fun observeTrashSize(): Flow<Long> =
            rows.map { map -> map.values.sumOf { it.sizeBytes } }

        override fun canTrash(path: String): Boolean = true

        override suspend fun moveToTrash(paths: List<String>): Result<Int> = 0.asSuccess()

        override suspend fun restore(ids: List<String>): Result<Int> {
            rows.value = rows.value - ids.toSet()
            return ids.size.asSuccess()
        }

        override suspend fun deleteForever(ids: List<String>): Result<Int> {
            rows.value = rows.value - ids.toSet()
            return ids.size.asSuccess()
        }

        override suspend fun emptyTrash(): Result<Int> {
            val count = rows.value.size
            rows.value = emptyMap()
            return count.asSuccess()
        }

        override suspend fun purgeExpired(retention: TrashRetention): Result<Int> {
            purgeCalls++
            return 0.asSuccess()
        }
    }

    private fun item(
        id: String,
        deletedAt: Long = 0,
        size: Long = 10,
    ) = TrashedItem(
        id = id,
        originalPath = "/sd/$id",
        name = "$id.txt",
        isDirectory = false,
        sizeBytes = size,
        deletedAtEpochMillis = deletedAt,
    )

    private fun viewModel(trash: TrashRepository) =
        RecycleBinViewModel(
            observeTrash = ObserveTrashUseCase(trash),
            observeTrashSize = ObserveTrashSizeUseCase(trash),
            restoreFromTrash = RestoreFromTrashUseCase(trash),
            deleteForever = DeleteForeverUseCase(trash),
            emptyTrash = EmptyTrashUseCase(trash),
            purgeExpiredTrash = PurgeExpiredTrashUseCase(trash),
        )

    @Test
    fun `state streams the trashed items newest-first with total size`() =
        runTest(dispatcher) {
            val trash = FakeTrash(listOf(item("a", deletedAt = 100, size = 5), item("b", deletedAt = 200, size = 7)))
            val vm = viewModel(trash)

            vm.uiState.test {
                assertThat(awaitItem().isLoading).isTrue() // initial seed
                val loaded = awaitItem()
                assertThat(loaded.isLoading).isFalse()
                assertThat(loaded.items.map { it.id }).containsExactly("b", "a").inOrder()
                assertThat(loaded.totalSizeBytes).isEqualTo(12L)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `opening the bin auto-purges expired items`() =
        runTest(dispatcher) {
            val trash = FakeTrash()
            val vm = viewModel(trash)

            // Keep a subscriber so the VM stays active, then let init's purge run.
            vm.uiState.test {
                awaitItem()
                advanceUntilIdle()
                cancelAndIgnoreRemainingEvents()
            }

            assertThat(trash.purgeCalls).isEqualTo(1)
        }

    @Test
    fun `restore removes the item and posts a message`() =
        runTest(dispatcher) {
            val trash = FakeTrash(listOf(item("a"), item("b")))
            val vm = viewModel(trash)

            vm.uiState.test {
                awaitItem() // seed
                awaitItem() // both items
                vm.restore("a")
                advanceUntilIdle()

                val after = expectMostRecentItem()
                assertThat(after.items.map { it.id }).containsExactly("b")
                assertThat(after.message?.res).isEqualTo(R.string.recycle_bin_restored)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `emptyBin clears everything and posts a message`() =
        runTest(dispatcher) {
            val trash = FakeTrash(listOf(item("a"), item("b")))
            val vm = viewModel(trash)

            vm.uiState.test {
                awaitItem()
                awaitItem()
                vm.emptyBin()
                advanceUntilIdle()

                val after = expectMostRecentItem()
                assertThat(after.items).isEmpty()
                assertThat(after.isEmpty).isTrue()
                assertThat(after.message?.res).isEqualTo(R.string.recycle_bin_emptied)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `a failed permanent delete posts the failure message`() =
        runTest(dispatcher) {
            val failing =
                object : TrashRepository by FakeTrash(listOf(item("a"))) {
                    override suspend fun deleteForever(ids: List<String>): Result<Int> =
                        OperationError.Io().asError()
                }
            val vm = viewModel(failing)

            vm.uiState.test {
                awaitItem()
                awaitItem()
                vm.deleteForever("a")
                advanceUntilIdle()

                assertThat(expectMostRecentItem().message?.res).isEqualTo(R.string.recycle_bin_delete_failed)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `consumeMessage clears the transient message`() =
        runTest(dispatcher) {
            val trash = FakeTrash(listOf(item("a")))
            val vm = viewModel(trash)

            vm.uiState.test {
                awaitItem()
                awaitItem()
                vm.restore("a")
                advanceUntilIdle()
                assertThat(expectMostRecentItem().message).isNotNull()

                vm.consumeMessage()
                advanceUntilIdle()
                assertThat(expectMostRecentItem().message).isNull()
                cancelAndIgnoreRemainingEvents()
            }
        }
}
