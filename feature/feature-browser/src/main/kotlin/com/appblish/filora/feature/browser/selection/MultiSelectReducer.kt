package com.appblish.filora.feature.browser.selection

/**
 * Pure state transitions for the browser multi-selection. Kept separate from
 * [MultiSelectController] so the long-press / select-all / clear-all semantics can be
 * unit-tested without any coroutine or Compose machinery.
 */
object MultiSelectReducer {
    /**
     * Toggles [path]'s membership. Selecting an absent path adds it (entering selection
     * mode on the first item); selecting a present path removes it. [isDirectory] is
     * recorded on add so the action bar can reason about directory-only restrictions.
     */
    fun toggle(
        state: SelectionState,
        path: String,
        isDirectory: Boolean
    ): SelectionState {
        val next = state.selected.toMutableMap()
        if (next.remove(path) == null) {
            next[path] = isDirectory
        }
        return state.copy(selected = next)
    }

    /**
     * Adds every entry in [entries] (path -> isDirectory) to the selection, leaving any
     * existing selection in place. A no-op for an empty input.
     */
    fun selectAll(
        state: SelectionState,
        entries: Map<String, Boolean>
    ): SelectionState {
        if (entries.isEmpty()) return state
        val next = state.selected.toMutableMap()
        next.putAll(entries)
        return state.copy(selected = next)
    }

    /** Clears the selection, exiting selection mode. */
    fun clear(): SelectionState = SelectionState()
}
