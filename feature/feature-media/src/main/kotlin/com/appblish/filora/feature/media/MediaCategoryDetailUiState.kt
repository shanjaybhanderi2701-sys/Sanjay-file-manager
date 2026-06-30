package com.appblish.filora.feature.media

import com.appblish.filora.core.domain.model.FileItem

/**
 * Immutable state for a single category's detail list (FR-6.1). While [isLoading]
 * the screen shows a spinner; otherwise it renders [items]. [errorMessage] is
 * non-null when the category load failed (e.g. permission revoked); [items] is then
 * empty so the empty/error state shows instead of a stale list.
 */
data class MediaCategoryDetailUiState(
    val isLoading: Boolean = true,
    val items: List<FileItem> = emptyList(),
    val errorMessage: String? = null,
) {
    /** True once a load completed with no error and no entries — an empty category. */
    val isEmpty: Boolean get() = !isLoading && errorMessage == null && items.isEmpty()
}
