package com.appblish.filora.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appblish.filora.core.common.result.Result
import com.appblish.filora.core.domain.repository.MediaAccess
import com.appblish.filora.core.domain.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Home dashboard ViewModel (M4 T4.6). Loads the per-category counts that back the
 * Home media section, gated on [MediaAccess]: without read access it publishes a
 * permission-required state instead of querying MediaStore (which would otherwise
 * surface as a generic error). [refresh] is idempotent and re-run from `onResume`
 * so a grant the user toggled in system settings is reflected the moment they
 * return.
 *
 * Volumes, favorites and recents stay empty here; they land with M6.
 */
@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val mediaRepository: MediaRepository,
        private val mediaAccess: MediaAccess,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(HomeUiState())
        val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

        init {
            refresh()
        }

        /** Reloads category counts; safe to call repeatedly (init + every onResume). */
        fun refresh() {
            if (!mediaAccess.hasReadAccess()) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        permissionRequired = true,
                        categoryCounts = emptyMap(),
                        errorMessage = null,
                    )
                }
                return
            }

            _uiState.update {
                it.copy(isLoading = true, permissionRequired = false, errorMessage = null)
            }
            viewModelScope.launch {
                when (val result = mediaRepository.categoryCounts()) {
                    is Result.Success ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                permissionRequired = false,
                                categoryCounts = result.data,
                                errorMessage = null,
                            )
                        }

                    is Result.Error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                permissionRequired = false,
                                categoryCounts = emptyMap(),
                                errorMessage = "Couldn't load your library. Pull to refresh.",
                            )
                        }
                }
            }
        }
    }
