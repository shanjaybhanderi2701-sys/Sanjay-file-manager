package com.appblish.filora.feature.storage

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appblish.filora.core.common.result.OperationError
import com.appblish.filora.core.common.result.Result
import com.appblish.filora.core.domain.model.FileItem
import com.appblish.filora.core.domain.usecase.GetLargestFilesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the largest-files view (FR-8.2). [load] scans a volume's top-N files via
 * [GetLargestFilesUseCase]; the volume id arrives from navigation, so the screen
 * binds it once and re-binding the same volume is a no-op (a different volume rescans).
 *
 * Delete/share are dispatched from the screen against the OS (see
 * [LargestFileActions]); when a file is actually removed the screen calls
 * [onDeleted] so the row disappears without a full rescan. [refresh] re-runs the scan
 * (e.g. after returning to the screen) to pick up files added or removed elsewhere.
 */
@HiltViewModel
class LargestFilesViewModel
    @Inject
    constructor(
        private val getLargestFiles: GetLargestFilesUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(LargestFilesUiState())
        val uiState: StateFlow<LargestFilesUiState> = _uiState.asStateFlow()

        private var boundVolumeId: String? = null
        private var hasBound = false

        /** Starts scanning [volumeId] (null = primary volume); idempotent per volume. */
        fun load(volumeId: String?) {
            if (hasBound && boundVolumeId == volumeId) return
            hasBound = true
            boundVolumeId = volumeId
            scan()
        }

        /** Re-runs the scan for the currently bound volume. */
        fun refresh() {
            if (!hasBound) return
            scan()
        }

        /** Drops [item] from the list after it was deleted from the device. */
        fun onDeleted(item: FileItem) {
            _uiState.update { state -> state.copy(files = state.files.filterNot { it.path == item.path }) }
        }

        private fun scan() {
            _uiState.update { it.copy(isLoading = true, errorMessageRes = null) }
            viewModelScope.launch {
                when (val result = getLargestFiles(volumeId = boundVolumeId)) {
                    is Result.Success ->
                        _uiState.update {
                            it.copy(isLoading = false, files = result.data, errorMessageRes = null)
                        }

                    is Result.Error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                files = emptyList(),
                                errorMessageRes = result.error.toMessageRes(),
                            )
                        }
                }
            }
        }
    }

@StringRes
private fun OperationError.toMessageRes(): Int =
    when (this) {
        is OperationError.PermissionDenied -> R.string.storage_largest_error_permission
        is OperationError.NotFound -> R.string.storage_largest_error_not_found
        else -> R.string.storage_largest_error_generic
    }
