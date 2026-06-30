package com.appblish.filora.core.data.operations

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WorkRequestStoreTest {
    private val store = WorkRequestStore()

    @Test
    fun `get returns the stashed list`() {
        val paths = listOf("a", "b", "c")
        store.put("op-1", paths)

        assertThat(store.get("op-1")).containsExactlyElementsIn(paths).inOrder()
    }

    @Test
    fun `get is null for an unknown key`() {
        assertThat(store.get("missing")).isNull()
    }

    @Test
    fun `remove evicts the list`() {
        store.put("op-1", listOf("a"))
        store.remove("op-1")

        assertThat(store.get("op-1")).isNull()
        assertThat(store.size).isEqualTo(0)
    }

    @Test
    fun `put copies the source list so later mutation does not leak in`() {
        val mutable = mutableListOf("a", "b")
        store.put("op-1", mutable)
        mutable.add("c")

        assertThat(store.get("op-1")).containsExactly("a", "b").inOrder()
    }
}
