package com.appblish.filora.feature.storage

import androidx.annotation.StringRes
import com.appblish.filora.core.domain.model.FileItem

/**
 * Immutable state for the largest-files view (FR-8.2). [files] is already ordered
 * largest-first by the use case; [errorMessageRes] is set only when the scan fails,
 * and an empty [files] with no error is the "nothing big here" case the screen
 * renders as an empty state.
 */
data class LargestFilesUiState(
    val isLoading: Boolean = true,
    val files: List<FileItem> = emptyList(),
    @StringRes val errorMessageRes: Int? = null,
)
