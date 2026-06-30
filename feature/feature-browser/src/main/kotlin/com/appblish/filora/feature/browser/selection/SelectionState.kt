package com.appblish.filora.feature.browser.selection

/**
 * Immutable snapshot of the browser multi-selection (FR-4.1). [selected] maps a
 * selected item's opaque path to whether it is a directory — enough for the action
 * bar to show a count and decide which batch actions are enabled, without retaining
 * full [com.appblish.filora.core.domain.model.FileItem]s.
 *
 * Selection mode is derived: it is active iff at least one item is selected, so
 * clearing the last item exits selection mode (matching the Back-clears-selection
 * rule in docs/phase-1/06-navigation-flow.md).
 */
data class SelectionState(
    val selected: Map<String, Boolean> = emptyMap(),
) {
    val count: Int get() = selected.size

    val isActive: Boolean get() = selected.isNotEmpty()

    /** True when any selected entry is a directory. */
    val containsDirectory: Boolean get() = selected.values.any { it }

    /** Batch actions currently enabled for this selection (FR-4.1). */
    val enabledActions: Set<BatchAction> get() = BatchAction.enabledFor(this)

    fun isSelected(path: String): Boolean = selected.containsKey(path)
}
