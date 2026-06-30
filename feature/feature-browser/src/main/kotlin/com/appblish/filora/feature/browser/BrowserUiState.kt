package com.appblish.filora.feature.browser

import androidx.annotation.StringRes
import com.appblish.filora.core.domain.model.FileItem
import com.appblish.filora.core.domain.model.SortOrder
import com.appblish.filora.core.domain.model.ViewLayout

/**
 * Everything the Browser screen renders (T036, FR-2.1). [entries] is the already
 * ordered-and-hidden-filtered list the UI draws; [phase] is the single source of truth
 * for which of the loading/empty/error/content surfaces shows (T044). Layout, sort, and
 * show-hidden mirror the persisted preferences so the toolbar reflects them without a
 * second read.
 */
data class BrowserUiState(
    val location: String = "",
    val phase: Phase = Phase.Loading,
    val entries: List<FileItem> = emptyList(),
    val layout: ViewLayout = ViewLayout.List,
    val sortOrder: SortOrder = SortOrder.Default,
    val showHidden: Boolean = false,
    val isRefreshing: Boolean = false,
    @StringRes val errorMessageRes: Int? = null,
) {
    enum class Phase { Loading, Content, Empty, Error }

    val isContent: Boolean get() = phase == Phase.Content
    val isEmpty: Boolean get() = phase == Phase.Empty
    val isLoading: Boolean get() = phase == Phase.Loading
    val isError: Boolean get() = phase == Phase.Error
}
