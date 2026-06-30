package com.appblish.filora.feature.media

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appblish.filora.core.common.result.OperationError
import com.appblish.filora.core.common.result.Result
import com.appblish.filora.core.domain.model.MediaCategory
import com.appblish.filora.core.domain.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives a single category's detail list (FR-6.1). Observes
 * [MediaRepository.observeCategory] for the bound category and republishes each
 * emission as [MediaCategoryDetailUiState]. The category arrives from navigation, so
 * the screen [bind]s it once; re-binding the same category is a no-op, and binding a
 * new one cancels the previous collection so only the visible category streams.
 */
@HiltViewModel
class MediaCategoryDetailViewModel
    @Inject
    constructor(
        private val mediaRepository: MediaRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(MediaCategoryDetailUiState())
        val uiState: StateFlow<MediaCategoryDetailUiState> = _uiState.asStateFlow()

        private var boundCategory: MediaCategory? = null
        private var observeJob: Job? = null

        /** Starts observing [category]; safe to call repeatedly with the same value. */
        fun bind(category: MediaCategory) {
            if (category == boundCategory) return
            boundCategory = category
            observeJob?.cancel()
            _uiState.update {
                it.copy(isLoading = true, items = emptyList(), errorMessage = null)
            }
            observeJob =
                viewModelScope.launch {
                    mediaRepository.observeCategory(category).collect { result ->
                        _uiState.update {
                            when (result) {
                                is Result.Success ->
                                    it.copy(
                                        isLoading = false,
                                        items = result.data,
                                        errorMessage = null,
                                    )

                                is Result.Error ->
                                    it.copy(
                                        isLoading = false,
                                        items = emptyList(),
                                        errorMessage = result.error.toDetailMessage(),
                                    )
                            }
                        }
                    }
                }
        }
    }

private fun OperationError.toDetailMessage(): String =
    when (this) {
        is OperationError.PermissionDenied -> "Grant storage access to see these files."
        else -> "Couldn't load this category. Pull to refresh."
    }
