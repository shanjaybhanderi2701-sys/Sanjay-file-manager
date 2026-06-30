package com.appblish.filora.feature.browser.dialog

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Singular/plural and trash-vs-permanent copy for the delete confirmation (FR-3.4). */
class DeleteConfirmationTest {
    @Test
    fun `single file to trash reads in the singular and is non-destructive`() {
        val prompt = DeleteConfirmation.forSelection(count = 1, containsDirectory = false, toTrash = true)

        assertThat(prompt.title).isEqualTo("Move to trash")
        assertThat(prompt.confirmLabel).isEqualTo("Move to trash")
        assertThat(prompt.message).isEqualTo("Move this file to the trash? You can restore it later.")
        assertThat(prompt.destructive).isFalse()
    }

    @Test
    fun `single folder to trash names a folder`() {
        val prompt = DeleteConfirmation.forSelection(count = 1, containsDirectory = true, toTrash = true)

        assertThat(prompt.message).isEqualTo("Move this folder to the trash? You can restore it later.")
    }

    @Test
    fun `multiple items pluralise the count and pronoun`() {
        val prompt = DeleteConfirmation.forSelection(count = 3, containsDirectory = true, toTrash = true)

        assertThat(prompt.message).isEqualTo("Move 3 items to the trash? You can restore them later.")
    }

    @Test
    fun `permanent delete warns and is marked destructive`() {
        val prompt = DeleteConfirmation.forSelection(count = 2, containsDirectory = false, toTrash = false)

        assertThat(prompt.title).isEqualTo("Delete permanently")
        assertThat(prompt.confirmLabel).isEqualTo("Delete")
        assertThat(prompt.message).isEqualTo("Permanently delete 2 items? This can't be undone.")
        assertThat(prompt.destructive).isTrue()
    }
}
