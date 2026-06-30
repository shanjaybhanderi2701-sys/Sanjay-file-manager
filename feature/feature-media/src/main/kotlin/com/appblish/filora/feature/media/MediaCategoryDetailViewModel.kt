package com.appblish.filora.feature.media

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appblish.filora.core.common.result.OperationError
import com.appblish.filora.core.common.result.Result
import com.appblish.filora.core.domain.model.FileItem
import com.appblish.filora.core.domain.model.MediaCategory
import com.appblish.filora.core.domain.repository.MediaRepository
import com.appblish.filora.core.domain.usecase.ObserveFavoritesUseCase
import com.appblish.filora.core.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives a single category's detail list (FR-6.1). Observes
 * [MediaRepository.observeCategory] for the bound category and republishes each
 * emission as [MediaCategoryDetailUiState]. The category arrives from navigation, so
 * the screen [bind]s it once; re-binding the same category is a no-op, and binding a
 * new one cancels the previous collection so only the visible category streams.
 *
 * Favorites (FR-9.1, T094): the pinned-path set is observed from the Room-backed
 * [ObserveFavoritesUseCase] so each row can offer "pin" or "unpin" in its context
 * menu; [toggleFavorite] flips the pin via [ToggleFavoriteUseCase].
 */
@HiltViewModel
class MediaCategoryDetailViewModel
    @Inject
    constructor(
        private val mediaRepository: MediaRepository,
        private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
        observeFavorites: ObserveFavoritesUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(MediaCategoryDetailUiState())
        val uiState: StateFlow<MediaCategoryDetailUiState> = _uiState.asStateFlow()

        private var boundCategory: MediaCategory? = null
        private var observeJob: Job? = null

        init {
            observeFavorites()
                .onEach { favorites ->
                    val paths = favorites.mapTo(mutableSetOf(), FileItem::path)
                    _uiState.update { it.copy(favoritePaths = paths) }
                }.launchIn(viewModelScope)
        }

        /** Starts observing [category]; safe to call repeatedly with the same value. */
        fun bind(category: MediaCategory) {
            if (category == boundCategory) return
            boundCategory = category
            observeJob?.cancel()
            _uiState.update {
                it.copy(isLoading = true, items = emptyList(), errorMessageRes = null)
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
                                        errorMessageRes = null,
                                    )

                                is Result.Error ->
                                    it.copy(
                                        isLoading = false,
                                        items = emptyList(),
                                        errorMessageRes = result.error.toDetailMessageRes(),
                                    )
                            }
                        }
                    }
                }
        }

        /** Pins or unpins [item] (FR-9.1); the observed favorites stream updates the UI. */
        fun toggleFavorite(item: FileItem) {
            viewModelScope.launch { toggleFavoriteUseCase(item) }
        }
    }

@StringRes
private fun OperationError.toDetailMessageRes(): Int =
    when (this) {
        is OperationError.PermissionDenied -> R.string.media_detail_error_permission
        else -> R.string.media_detail_error_load
    }
