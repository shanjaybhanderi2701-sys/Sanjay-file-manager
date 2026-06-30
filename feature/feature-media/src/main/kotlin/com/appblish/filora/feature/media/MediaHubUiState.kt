package com.appblish.filora.feature.media

import androidx.annotation.StringRes

/**
 * Immutable state for the Media category-hub screen. While [isLoading] the screen
 * shows a spinner; otherwise it renders [tiles] (always the full seven hubs once a
 * load completes). [errorMessageRes] is a non-null string resource when the count
 * load failed (resolved at the Composable render site, NFR-7) — tiles still render
 * with zero counts so the hubs stay navigable.
 */
data class MediaHubUiState(
    val isLoading: Boolean = true,
    val tiles: List<CategoryHubTile> = emptyList(),
    @StringRes val errorMessageRes: Int? = null,
)
