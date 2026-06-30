package com.appblish.filora.feature.browser.dialog

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import com.appblish.filora.feature.browser.R

/**
 * Copy for the delete confirmation dialog (FR-3.4 — "confirmation required"). Kept
 * free of Compose/Android runtime so the singular/plural and trash-vs-permanent
 * wording stays pure-JVM testable; only string-resource ids (compile-time `int`
 * constants) cross the boundary. The [DeleteConfirmDialog] composable resolves and
 * renders what this produces (NFR-7 — no hardcoded user-facing strings).
 *
 * [messageIsPlural] selects how [messageRes] is resolved: a `plurals` resource via
 * `pluralStringResource(messageRes, count, count)` when true, otherwise a plain
 * `string` resource. [destructive] is true only for a permanent (non-recoverable)
 * delete, so the dialog can style the confirm button as a warning; a trash move is
 * reversible and reads calmly.
 */
data class DeletePrompt(
    @StringRes val titleRes: Int,
    val messageRes: Int,
    val messageIsPlural: Boolean,
    val count: Int,
    @StringRes val confirmLabelRes: Int,
    val destructive: Boolean,
)

object DeleteConfirmation {
    /**
     * Builds the confirmation for deleting [count] selected items, [containsDirectory]
     * of which at least one is a folder. When [toTrash] is true the items move to a
     * recoverable trash (NFR-2.4); otherwise the copy warns the action is permanent.
     *
     * Singular selections name the item kind ("this folder" / "this file"); batches
     * use a quantity-aware `plurals` resource so the count and pronoun agree per the
     * device locale's plural rules.
     */
    fun forSelection(
        count: Int,
        containsDirectory: Boolean,
        toTrash: Boolean,
    ): DeletePrompt {
        val single = count <= 1
        @StringRes @PluralsRes val messageRes =
            when {
                toTrash && single && containsDirectory -> R.string.browser_delete_trash_folder
                toTrash && single -> R.string.browser_delete_trash_file
                toTrash -> R.plurals.browser_delete_trash_items
                single && containsDirectory -> R.string.browser_delete_perm_folder
                single -> R.string.browser_delete_perm_file
                else -> R.plurals.browser_delete_perm_items
            }
        return DeletePrompt(
            titleRes = if (toTrash) R.string.browser_delete_trash_title else R.string.browser_delete_perm_title,
            messageRes = messageRes,
            messageIsPlural = !single,
            count = count,
            confirmLabelRes =
                if (toTrash) R.string.browser_delete_trash_confirm else R.string.browser_delete_perm_confirm,
            destructive = !toTrash,
        )
    }
}
