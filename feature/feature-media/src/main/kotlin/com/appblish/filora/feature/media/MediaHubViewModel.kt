package com.appblish.filora.feature.media

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appblish.filora.core.common.result.OperationError
import com.appblish.filora.core.common.result.Result
import com.appblish.filora.core.domain.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the Media category-hub screen (FR-6.1). Loads per-category counts from
 * [MediaRepository] and maps them onto the fixed seven-hub grid. On failure it still
 * publishes the seven hubs (zero counts) plus a user-facing [MediaHubUiState.errorMessageRes].
 */
@HiltViewModel
class MediaHubViewModel
    @Inject
    constructor(
        private val mediaRepository: MediaRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(MediaHubUiState())
        val uiState: StateFlow<MediaHubUiState> = _uiState.asStateFlow()

        init {
            refresh()
        }

        /** Reloads counts; safe to call again from a retry affordance. */
        fun refresh() {
            _uiState.update { it.copy(isLoading = true, errorMessageRes = null) }
            viewModelScope.launch {
                when (val result = mediaRepository.categoryCounts()) {
                    is Result.Success ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                tiles = buildHubTiles(result.data),
                                errorMessageRes = null,
                            )
                        }

                    is Result.Error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                tiles = buildHubTiles(emptyMap()),
                                errorMessageRes = result.error.toUserMessageRes(),
                            )
                        }
                }
            }
        }
    }

@StringRes
private fun OperationError.toUserMessageRes(): Int =
    when (this) {
        is OperationError.PermissionDenied -> R.string.media_error_permission
        else -> R.string.media_error_load
    }
