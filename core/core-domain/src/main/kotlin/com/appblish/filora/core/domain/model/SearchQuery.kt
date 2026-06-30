package com.appblish.filora.core.domain.model

/** Parameters for a file search. */
data class SearchQuery(
    val text: String,
    val rootPath: String? = null,
    val categories: Set<MediaCategory> = emptySet(),
    val includeHidden: Boolean = false,
    val sortOrder: SortOrder = SortOrder.Default,
) {
    val isBlank: Boolean get() = text.isBlank() && categories.isEmpty()
}
