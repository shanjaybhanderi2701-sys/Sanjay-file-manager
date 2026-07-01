package com.appblish.filora.feature.storage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appblish.filora.core.common.result.Result
import com.appblish.filora.core.domain.usecase.DeleteForeverUseCase
import com.appblish.filora.core.domain.usecase.EmptyTrashUseCase
import com.appblish.filora.core.domain.usecase.ObserveTrashSizeUseCase
import com.appblish.filora.core.domain.usecase.ObserveTrashUseCase
import com.appblish.filora.core.domain.usecase.PurgeExpiredTrashUseCase
import com.appblish.filora.core.domain.usecase.RestoreFromTrashUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the Recycle Bin (FR-3.4, T125/T129). The list and its total size are streamed
 * from the app-managed trash, so restore / permanent-delete / empty need not refresh
 * manually — the Room-backed flows re-emit. Retention auto-purge (T128) runs once when
 * the screen is first observed, so stale items age out even if the user only ever opens
 * the bin.
 *
 * Actions surface a one-shot [RecycleBinMessage] the screen consumes; the destructive
 * confirmations (permanent delete / empty) are the screen's responsibility.
 */
@HiltViewModel
class RecycleBinViewModel
    @Inject
    constructor(
        observeTrash: ObserveTrashUseCase,
        observeTrashSize: ObserveTrashSizeUseCase,
        private val restoreFromTrash: RestoreFromTrashUseCase,
        private val deleteForever: DeleteForeverUseCase,
        private val emptyTrash: EmptyTrashUseCase,
        private val purgeExpiredTrash: PurgeExpiredTrashUseCase,
    ) : ViewModel() {
        private val message = MutableStateFlow<RecycleBinMessage?>(null)

        val uiState: StateFlow<RecycleBinUiState> =
            combine(observeTrash(), observeTrashSize(), message) { items, size, msg ->
                RecycleBinUiState(
                    isLoading = false,
                    items = items,
                    totalSizeBytes = size,
                    message = msg,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = RecycleBinUiState(),
            )

        init {
            // Auto-purge expired items opportunistically when the bin is opened (T128).
            viewModelScope.launch { purgeExpiredTrash() }
        }

        /** Restores [id] to its original location. */
        fun restore(id: String) {
            viewModelScope.launch {
                message.value =
                    when (restoreFromTrash(listOf(id))) {
                        is Result.Success -> RecycleBinMessage(R.string.recycle_bin_restored, count = 1)
                        is Result.Error -> RecycleBinMessage(R.string.recycle_bin_restore_failed, count = 1)
                    }
            }
        }

        /** Permanently deletes [id] from the bin (irreversible; screen confirms first). */
        fun deleteForever(id: String) {
            viewModelScope.launch {
                message.value =
                    when (deleteForever(listOf(id))) {
                        is Result.Success -> RecycleBinMessage(R.string.recycle_bin_deleted, count = 1)
                        is Result.Error -> RecycleBinMessage(R.string.recycle_bin_delete_failed, count = 1)
                    }
            }
        }

        /** Empties the whole bin (irreversible; screen confirms first). */
        fun emptyBin() {
            viewModelScope.launch {
                message.value =
                    when (val result = emptyTrash()) {
                        is Result.Success -> RecycleBinMessage(R.string.recycle_bin_emptied, count = result.data)
                        is Result.Error -> RecycleBinMessage(R.string.recycle_bin_empty_failed)
                    }
            }
        }

        fun consumeMessage() {
            message.update { null }
        }

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }
