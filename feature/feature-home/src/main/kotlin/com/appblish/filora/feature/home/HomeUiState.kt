package com.appblish.filora.feature.home

import com.appblish.filora.core.domain.model.FileItem
import com.appblish.filora.core.domain.model.MediaCategory
import com.appblish.filora.core.domain.model.StorageVolume

/**
 * Immutable Home dashboard state. M1 renders the empty case; volumes, categories,
 * favorites and recents are populated once the data layer lands.
 */
data class HomeUiState(
    val isLoading: Boolean = false,
    val volumes: List<StorageVolume> = emptyList(),
    val categoryCounts: Map<MediaCategory, Int> = emptyMap(),
    val favorites: List<FileItem> = emptyList(),
    val recents: List<FileItem> = emptyList(),
) {
    val isEmpty: Boolean
        get() =
            !isLoading &&
                volumes.isEmpty() &&
                categoryCounts.isEmpty() &&
                favorites.isEmpty() &&
                recents.isEmpty()
}
