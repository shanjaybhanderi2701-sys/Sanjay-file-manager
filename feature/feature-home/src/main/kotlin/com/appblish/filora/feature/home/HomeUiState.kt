package com.appblish.filora.feature.home

import androidx.annotation.StringRes
import com.appblish.filora.core.domain.model.FileItem
import com.appblish.filora.core.domain.model.MediaCategory
import com.appblish.filora.core.domain.model.StorageVolume

/**
 * Immutable Home dashboard state.
 *
 * M4 (T4.6) wires the [categoryCounts] section: per-category item counts loaded
 * from `MediaRepository`, gated on media access. When access is missing
 * [permissionRequired] is true and the screen shows a "grant access" prompt rather
 * than zeroed tiles; an `onResume` re-query flips it back once the user grants.
 * [volumes], [favorites] and [recents] are populated in M6.
 */
data class HomeUiState(
    val isLoading: Boolean = false,
    val permissionRequired: Boolean = false,
    val volumes: List<StorageVolume> = emptyList(),
    val categoryCounts: Map<MediaCategory, Int> = emptyMap(),
    val favorites: List<FileItem> = emptyList(),
    val recents: List<FileItem> = emptyList(),
    @StringRes val errorMessageRes: Int? = null,
) {
    /** True when a load completed with access but produced nothing to show. */
    val isEmpty: Boolean
        get() =
            !isLoading &&
                !permissionRequired &&
                errorMessageRes == null &&
                volumes.isEmpty() &&
                categoryCounts.values.all { it == 0 } &&
                favorites.isEmpty() &&
                recents.isEmpty()
}
