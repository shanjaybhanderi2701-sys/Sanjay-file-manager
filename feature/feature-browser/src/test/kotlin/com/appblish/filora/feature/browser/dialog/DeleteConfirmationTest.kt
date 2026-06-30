package com.appblish.filora.feature.browser.dialog

import com.appblish.filora.feature.browser.R
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Singular/plural and trash-vs-permanent copy selection for the delete confirmation
 * (FR-3.4). Asserts the chosen string-resource ids and quantity flags; the literal
 * wording lives in `strings.xml` (NFR-7) and the dialog resolves it.
 */
class DeleteConfirmationTest {
    @Test
    fun `single file to trash names a file and is non-destructive`() {
        val prompt = DeleteConfirmation.forSelection(count = 1, containsDirectory = false, toTrash = true)

        assertThat(prompt.titleRes).isEqualTo(R.string.browser_delete_trash_title)
        assertThat(prompt.confirmLabelRes).isEqualTo(R.string.browser_delete_trash_confirm)
        assertThat(prompt.messageRes).isEqualTo(R.string.browser_delete_trash_file)
        assertThat(prompt.messageIsPlural).isFalse()
        assertThat(prompt.destructive).isFalse()
    }

    @Test
    fun `single folder to trash names a folder`() {
        val prompt = DeleteConfirmation.forSelection(count = 1, containsDirectory = true, toTrash = true)

        assertThat(prompt.messageRes).isEqualTo(R.string.browser_delete_trash_folder)
        assertThat(prompt.messageIsPlural).isFalse()
    }

    @Test
    fun `multiple items use a quantity-aware trash message`() {
        val prompt = DeleteConfirmation.forSelection(count = 3, containsDirectory = true, toTrash = true)

        assertThat(prompt.messageRes).isEqualTo(R.plurals.browser_delete_trash_items)
        assertThat(prompt.messageIsPlural).isTrue()
        assertThat(prompt.count).isEqualTo(3)
    }

    @Test
    fun `permanent delete warns and is marked destructive`() {
        val prompt = DeleteConfirmation.forSelection(count = 2, containsDirectory = false, toTrash = false)

        assertThat(prompt.titleRes).isEqualTo(R.string.browser_delete_perm_title)
        assertThat(prompt.confirmLabelRes).isEqualTo(R.string.browser_delete_perm_confirm)
        assertThat(prompt.messageRes).isEqualTo(R.plurals.browser_delete_perm_items)
        assertThat(prompt.messageIsPlural).isTrue()
        assertThat(prompt.destructive).isTrue()
    }
}
