package com.appblish.filora.feature.browser.selection

import com.appblish.filora.core.domain.model.FileItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Observable owner of the browser multi-selection. The browser ViewModel (T2.2) holds
 * one of these and feeds list interactions into it: long-press or in-mode taps call
 * [toggle], the overflow calls [selectAll], and Back / the action bar's close call
 * [clear]. State transitions are delegated to the pure [MultiSelectReducer].
 */
class MultiSelectController {
    private val _state = MutableStateFlow(SelectionState())
    val state: StateFlow<SelectionState> = _state.asStateFlow()

    /** Long-press entry point and in-mode tap: toggles the item's selection. */
    fun toggle(item: FileItem) {
        _state.update { MultiSelectReducer.toggle(it, item.path, item.isDirectory) }
    }

    /** Select-all over the currently listed [items]. */
    fun selectAll(items: List<FileItem>) {
        _state.update { current ->
            MultiSelectReducer.selectAll(current, items.associate { it.path to it.isDirectory })
        }
    }

    /** Clears the selection and exits selection mode. */
    fun clear() {
        _state.value = MultiSelectReducer.clear()
    }
}
