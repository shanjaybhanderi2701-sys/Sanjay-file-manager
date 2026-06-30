package com.appblish.filora.feature.media

import androidx.annotation.StringRes
import com.appblish.filora.core.domain.model.FileItem

/**
 * Immutable state for a single category's detail list (FR-6.1). While [isLoading]
 * the screen shows a spinner; otherwise it renders [items]. [errorMessageRes] is a
 * non-null string resource when the category load failed (e.g. permission revoked),
 * resolved at the Composable render site (NFR-7); [items] is then empty so the
 * empty/error state shows instead of a stale list. [favoritePaths] is the set of
 * currently-pinned paths (FR-9.1, T094) so a row's context menu can show pin/unpin.
 */
data class MediaCategoryDetailUiState(
    val isLoading: Boolean = true,
    val items: List<FileItem> = emptyList(),
    val favoritePaths: Set<String> = emptySet(),
    @StringRes val errorMessageRes: Int? = null,
) {
    /** True once a load completed with no error and no entries — an empty category. */
    val isEmpty: Boolean get() = !isLoading && errorMessageRes == null && items.isEmpty()
}
