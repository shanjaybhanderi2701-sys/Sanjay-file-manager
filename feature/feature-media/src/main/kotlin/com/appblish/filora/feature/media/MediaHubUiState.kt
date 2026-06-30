package com.appblish.filora.feature.media

/**
 * Immutable state for the Media category-hub screen. While [isLoading] the screen
 * shows a spinner; otherwise it renders [tiles] (always the full seven hubs once a
 * load completes). [errorMessage] is non-null when the count load failed — tiles
 * still render with zero counts so the hubs stay navigable.
 */
data class MediaHubUiState(
    val isLoading: Boolean = true,
    val tiles: List<CategoryHubTile> = emptyList(),
    val errorMessage: String? = null,
)
