package com.appblish.filora.feature.storage

import androidx.annotation.StringRes
import com.appblish.filora.core.domain.model.TrashedItem

/**
 * Immutable state for the Recycle Bin (FR-3.4, T125/T129). [items] is streamed from the
 * app-managed trash newest-first; [totalSizeBytes] is the bin's footprint for the
 * empty-trash affordance. [message] is a one-shot user message (e.g. "Restored 3
 * items") the screen shows and then clears via [RecycleBinViewModel.consumeMessage].
 */
data class RecycleBinUiState(
    val isLoading: Boolean = true,
    val items: List<TrashedItem> = emptyList(),
    val totalSizeBytes: Long = 0L,
    val message: RecycleBinMessage? = null,
) {
    val isEmpty: Boolean get() = !isLoading && items.isEmpty()
}

/** A transient message: a string resource plus a count for its plural formatting. */
data class RecycleBinMessage(
    @StringRes val res: Int,
    val count: Int = 0,
)
