package com.appblish.filora.feature.search

import com.appblish.filora.core.domain.model.FileItem
import com.appblish.filora.core.domain.model.SearchFilter

/**
 * Immutable state for the Search screen (FR-5.1 streaming results, FR-5.2 filters).
 *
 * [results] are the name matches that survive the active [filter]; they grow as the
 * cancelable search stream emits. [selectedSize]/[selectedDate] are the single-choice
 * presets currently applied — held alongside [filter] (which only carries primitive
 * bounds) so the chip row can show which preset is active and remove it. [hasSearched]
 * flips true once a non-blank query is submitted, distinguishing the first-run idle
 * screen from a completed search that found nothing.
 */
data class SearchUiState(
    val query: String = "",
    val filter: SearchFilter = SearchFilter(),
    val selectedSize: SizeBucket? = null,
    val selectedDate: DateBucket? = null,
    val results: List<FileItem> = emptyList(),
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false,
) {
    /** A finished search for a non-blank query that yielded nothing (FR-5.1 empty state). */
    val isEmpty: Boolean
        get() = hasSearched && !isSearching && results.isEmpty()

    /** Active filters as removable chips (FR-5.2), in a stable type → size → date order. */
    val activeChips: List<ActiveFilterChip>
        get() =
            buildList {
                // Sort by ordinal so the chip row keeps a stable order across toggles.
                filter.types.sortedBy { it.ordinal }.forEach { add(ActiveFilterChip.Type(it)) }
                selectedSize?.let { add(ActiveFilterChip.Size(it)) }
                selectedDate?.let { add(ActiveFilterChip.Date(it)) }
            }
}
