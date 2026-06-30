package com.appblish.filora.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appblish.filora.core.common.result.Result
import com.appblish.filora.core.domain.repository.MediaAccess
import com.appblish.filora.core.domain.repository.MediaRepository
import com.appblish.filora.core.domain.usecase.ObserveFavoritesUseCase
import com.appblish.filora.core.domain.usecase.ObserveRecentsUseCase
import com.appblish.filora.core.domain.usecase.ObserveStorageVolumesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Home dashboard ViewModel (M4 T4.6, extended in M6 T6.2). Loads the per-category
 * counts that back the Home media section, gated on [MediaAccess]: without read
 * access it publishes a permission-required state instead of querying MediaStore
 * (which would otherwise surface as a generic error). [refresh] is idempotent and
 * re-run from `onResume` so a grant the user toggled in system settings is reflected
 * the moment they return.
 *
 * Favorites (FR-9.1) and recents (FR-9.2) are persisted in Room and observed
 * independently of the media permission, so the user's pins and recently-opened
 * entries show even before media access is granted. The two streams update their
 * own slice of [HomeUiState] as the user pins/unpins or opens files.
 *
 * Storage volumes (FR-12.1) are observed too, so Home is the single aggregate the spec
 * asks for — volumes + categories + favorites + recents (T6.5). The volume stream is a
 * lightweight used/free summary that taps through to the full storage breakdown screen
 * (T6.3); like favorites/recents it is independent of media access and re-emits as
 * volumes mount/unmount, satisfying FR-12.1's "reflects live storage state on resume".
 */
@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val mediaRepository: MediaRepository,
        private val mediaAccess: MediaAccess,
        observeFavorites: ObserveFavoritesUseCase,
        observeRecents: ObserveRecentsUseCase,
        observeStorageVolumes: ObserveStorageVolumesUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(HomeUiState())
        val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

        init {
            refresh()
            observeFavorites()
                .onEach { favorites -> _uiState.update { it.copy(favorites = favorites) } }
                .launchIn(viewModelScope)
            observeRecents()
                .onEach { recents -> _uiState.update { it.copy(recents = recents) } }
                .launchIn(viewModelScope)
            observeStorageVolumes()
                .onEach { volumes -> _uiState.update { it.copy(volumes = volumes) } }
                .launchIn(viewModelScope)
        }

        /** Reloads category counts; safe to call repeatedly (init + every onResume). */
        fun refresh() {
            if (!mediaAccess.hasReadAccess()) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        permissionRequired = true,
                        categoryCounts = emptyMap(),
                        errorMessageRes = null,
                    )
                }
                return
            }

            _uiState.update {
                it.copy(isLoading = true, permissionRequired = false, errorMessageRes = null)
            }
            viewModelScope.launch {
                when (val result = mediaRepository.categoryCounts()) {
                    is Result.Success ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                permissionRequired = false,
                                categoryCounts = result.data,
                                errorMessageRes = null,
                            )
                        }

                    is Result.Error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                permissionRequired = false,
                                categoryCounts = emptyMap(),
                                errorMessageRes = R.string.home_error_load,
                            )
                        }
                }
            }
        }
    }
