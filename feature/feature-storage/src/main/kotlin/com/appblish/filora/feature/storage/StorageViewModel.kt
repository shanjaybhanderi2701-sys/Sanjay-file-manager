package com.appblish.filora.feature.storage

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appblish.filora.core.common.result.Result
import com.appblish.filora.core.domain.model.StorageBreakdown
import com.appblish.filora.core.domain.usecase.GetStorageBreakdownUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Immutable state for the storage breakdown screen (FR-8.1). */
data class StorageUiState(
    val isLoading: Boolean = true,
    val breakdown: StorageBreakdown? = null,
    @StringRes val errorMessageRes: Int? = null,
)

/**
 * Drives the storage breakdown screen (FR-8.1). Collects
 * [GetStorageBreakdownUseCase] and publishes per-volume used/free with by-category
 * slices. A volume-enumeration failure surfaces as [StorageUiState.errorMessageRes]
 * while the screen stays renderable; missing media access simply yields volumes with
 * no category slices (the use case degrades it), so used/free always shows.
 */
@HiltViewModel
class StorageViewModel
    @Inject
    constructor(
        getStorageBreakdown: GetStorageBreakdownUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(StorageUiState())
        val uiState: StateFlow<StorageUiState> = _uiState.asStateFlow()

        init {
            viewModelScope.launch {
                getStorageBreakdown().collect { result ->
                    when (result) {
                        is Result.Success ->
                            _uiState.update {
                                it.copy(isLoading = false, breakdown = result.data, errorMessageRes = null)
                            }

                        is Result.Error ->
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessageRes = R.string.storage_error_load,
                                )
                            }
                    }
                }
            }
        }
    }
