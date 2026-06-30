package com.appblish.filora.feature.browser.selection

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Pure transition + enablement rules for the browser multi-selection (FR-4.1). */
class MultiSelectReducerTest {
    private val empty = SelectionState()

    @Test
    fun `empty selection is inactive with no enabled actions`() {
        assertThat(empty.isActive).isFalse()
        assertThat(empty.count).isEqualTo(0)
        assertThat(empty.enabledActions).isEmpty()
    }

    @Test
    fun `toggling a file adds it and enters selection mode`() {
        val state = MultiSelectReducer.toggle(empty, "/sd/a.txt", isDirectory = false)

        assertThat(state.isActive).isTrue()
        assertThat(state.count).isEqualTo(1)
        assertThat(state.isSelected("/sd/a.txt")).isTrue()
    }

    @Test
    fun `toggling the same path twice deselects and exits selection mode`() {
        val added = MultiSelectReducer.toggle(empty, "/sd/a.txt", isDirectory = false)
        val removed = MultiSelectReducer.toggle(added, "/sd/a.txt", isDirectory = false)

        assertThat(removed.count).isEqualTo(0)
        assertThat(removed.isActive).isFalse()
    }

    @Test
    fun `file-only selection enables every action including share`() {
        var state = MultiSelectReducer.toggle(empty, "/sd/a.txt", isDirectory = false)
        state = MultiSelectReducer.toggle(state, "/sd/b.txt", isDirectory = false)

        assertThat(state.count).isEqualTo(2)
        assertThat(state.containsDirectory).isFalse()
        assertThat(state.enabledActions).containsExactly(
            BatchAction.MOVE,
            BatchAction.COPY,
            BatchAction.DELETE,
            BatchAction.ZIP,
            BatchAction.SHARE,
        )
    }

    @Test
    fun `selecting a directory disables share but keeps the rest`() {
        var state = MultiSelectReducer.toggle(empty, "/sd/a.txt", isDirectory = false)
        state = MultiSelectReducer.toggle(state, "/sd/docs", isDirectory = true)

        assertThat(state.containsDirectory).isTrue()
        assertThat(state.enabledActions).containsExactly(
            BatchAction.MOVE,
            BatchAction.COPY,
            BatchAction.DELETE,
            BatchAction.ZIP,
        )
        assertThat(state.enabledActions).doesNotContain(BatchAction.SHARE)
    }

    @Test
    fun `selectAll adds all entries and is idempotent over existing selection`() {
        val entries = mapOf(
            "/sd/a.txt" to false,
            "/sd/b.txt" to false,
            "/sd/docs" to true,
        )
        val first = MultiSelectReducer.selectAll(empty, entries)
        val again = MultiSelectReducer.selectAll(first, entries)

        assertThat(first.count).isEqualTo(3)
        assertThat(again.count).isEqualTo(3)
        assertThat(first.containsDirectory).isTrue()
    }

    @Test
    fun `selectAll with no entries is a no-op`() {
        assertThat(MultiSelectReducer.selectAll(empty, emptyMap())).isEqualTo(empty)
    }

    @Test
    fun `clear resets to an empty selection`() {
        val state = MultiSelectReducer.selectAll(empty, mapOf("/sd/a.txt" to false))
        assertThat(MultiSelectReducer.clear()).isEqualTo(empty)
        assertThat(state.count).isEqualTo(1) // original snapshot is untouched
    }
}
