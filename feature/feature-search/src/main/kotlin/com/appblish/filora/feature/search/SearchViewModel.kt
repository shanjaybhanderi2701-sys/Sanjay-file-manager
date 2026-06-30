package com.appblish.filora.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appblish.filora.core.domain.model.FileItem
import com.appblish.filora.core.domain.model.FileTypeFilter
import com.appblish.filora.core.domain.model.SearchProgress
import com.appblish.filora.core.domain.model.SearchQuery
import com.appblish.filora.core.domain.repository.SearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the Search screen (FR-5.1 streaming + FR-5.2 type/size/date filters).
 *
 * Every change to the query text or a filter chip restarts the search: the in-flight
 * walk ([searchJob]) is cancelled and a fresh [SearchQuery] — carrying the active
 * [com.appblish.filora.core.domain.model.SearchFilter] so the type/size/date AND-combine
 * happens *inside* the walk ([SearchRepository]) rather than in the UI — is collected.
 * Query edits are debounced ([DEBOUNCE_MILLIS]) so a burst of keystrokes triggers a
 * single walk; chip taps apply immediately. Results stream in as [SearchProgress.Match]
 * emissions; the terminal [SearchProgress.Completed] clears the in-progress indicator.
 */
@HiltViewModel
class SearchViewModel
    @Inject
    constructor(
        private val searchRepository: SearchRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(SearchUiState())
        val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

        /**
         * Wall clock used to anchor relative date buckets ("Last 7 days"). Overridable
         * in tests so date filtering is deterministic without touching the real clock.
         */
        internal var nowProvider: () -> Long = { System.currentTimeMillis() }

        /** Search root (a path or tree-uri); set from the navigation scope. */
        private var rootPath: String? = null
        private var searchJob: Job? = null

        /** Binds the directory the search traverses; safe to call repeatedly. */
        fun bindScope(rootPath: String?) {
            this.rootPath = rootPath
        }

        fun onQueryChange(text: String) {
            _uiState.update { it.copy(query = text) }
            relaunchSearch(debounce = true)
        }

        /** Adds or removes [type] from the type dimension (multi-select, OR'd). */
        fun toggleType(type: FileTypeFilter) {
            _uiState.update { state ->
                val types =
                    state.filter.types.toMutableSet().apply {
                        if (!add(type)) remove(type)
                    }
                state.copy(filter = state.filter.copy(types = types))
            }
            relaunchSearch(debounce = false)
        }

        /** Applies a size preset, or clears it when [bucket] is null or already active. */
        fun selectSize(bucket: SizeBucket?) {
            _uiState.update { state ->
                val next = bucket.takeUnless { it == state.selectedSize }
                state.copy(
                    selectedSize = next,
                    filter = state.filter.copy(minSizeBytes = next?.minBytes, maxSizeBytes = next?.maxBytes),
                )
            }
            relaunchSearch(debounce = false)
        }

        /** Applies a date preset, or clears it when [bucket] is null or already active. */
        fun selectDate(bucket: DateBucket?) {
            _uiState.update { state ->
                val next = bucket.takeUnless { it == state.selectedDate }
                state.copy(
                    selectedDate = next,
                    filter = state.filter.copy(modifiedAfterEpochMillis = next?.afterBound(nowProvider())),
                )
            }
            relaunchSearch(debounce = false)
        }

        /** Clears exactly the dimension behind a removable chip (FR-5.2). */
        fun removeChip(chip: ActiveFilterChip) {
            when (chip) {
                is ActiveFilterChip.Type -> toggleType(chip.type)
                is ActiveFilterChip.Size -> selectSize(null)
                is ActiveFilterChip.Date -> selectDate(null)
            }
        }

        private fun relaunchSearch(debounce: Boolean) {
            searchJob?.cancel()
            val state = _uiState.value
            val query = SearchQuery(text = state.query, rootPath = rootPath, filter = state.filter)
            if (query.isBlank) {
                _uiState.update {
                    it.copy(results = emptyList(), isSearching = false, hasSearched = false)
                }
                return
            }
            _uiState.update { it.copy(results = emptyList(), isSearching = true, hasSearched = true) }
            searchJob =
                viewModelScope.launch {
                    if (debounce) delay(DEBOUNCE_MILLIS)
                    val matches = mutableListOf<FileItem>()
                    searchRepository.search(query).collect { progress ->
                        when (progress) {
                            is SearchProgress.Match -> {
                                matches.add(progress.item)
                                _uiState.update { it.copy(results = matches.toList()) }
                            }

                            is SearchProgress.Completed ->
                                _uiState.update { it.copy(isSearching = false) }
                        }
                    }
                }
        }

        private companion object {
            const val DEBOUNCE_MILLIS = 300L
        }
    }
