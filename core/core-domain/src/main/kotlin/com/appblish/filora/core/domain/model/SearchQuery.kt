package com.appblish.filora.core.domain.model

/**
 * Parameters for a file search. [filter] carries the type/size/date constraints
 * (FR-5.2); they AND-combine with the [text] substring during the walk, so a query can
 * narrow purely by size/date even with blank [text] (e.g. "files over 100 MB"). A query
 * with neither [text] nor an active [filter] (nor [categories]) is [isBlank] and the
 * search short-circuits to nothing.
 */
data class SearchQuery(
    val text: String,
    val rootPath: String? = null,
    val categories: Set<MediaCategory> = emptySet(),
    val filter: SearchFilter = SearchFilter(),
    val includeHidden: Boolean = false,
    val sortOrder: SortOrder = SortOrder.Default,
) {
    val isBlank: Boolean get() = text.isBlank() && categories.isEmpty() && filter.isEmpty
}
