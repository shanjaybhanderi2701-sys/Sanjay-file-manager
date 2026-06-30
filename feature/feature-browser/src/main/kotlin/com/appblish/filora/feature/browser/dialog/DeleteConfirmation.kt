package com.appblish.filora.feature.browser.dialog

/**
 * Copy for the delete confirmation dialog (FR-3.4 — "confirmation required"). Kept
 * free of Compose/Android types so the singular/plural and trash-vs-permanent
 * wording stays pure-JVM testable; the [DeleteConfirmDialog] composable only renders
 * what this produces.
 *
 * [destructive] is true only for a permanent (non-recoverable) delete, so the dialog
 * can style the confirm button as a warning. A trash move is reversible and reads
 * calmly.
 */
data class DeletePrompt(
    val title: String,
    val message: String,
    val confirmLabel: String,
    val destructive: Boolean,
)

object DeleteConfirmation {
    /**
     * Builds the confirmation for deleting [count] selected items, [containsDirectory]
     * of which at least one is a folder. When [toTrash] is true the items move to a
     * recoverable trash (NFR-2.4); otherwise the copy warns the action is permanent.
     */
    fun forSelection(
        count: Int,
        containsDirectory: Boolean,
        toTrash: Boolean,
    ): DeletePrompt {
        val noun =
            when {
                count <= 1 && containsDirectory -> "this folder"
                count <= 1 -> "this file"
                else -> "$count items"
            }
        val pronoun = if (count <= 1) "it" else "them"
        return if (toTrash) {
            DeletePrompt(
                title = "Move to trash",
                message = "Move $noun to the trash? You can restore $pronoun later.",
                confirmLabel = "Move to trash",
                destructive = false,
            )
        } else {
            DeletePrompt(
                title = "Delete permanently",
                message = "Permanently delete $noun? This can't be undone.",
                confirmLabel = "Delete",
                destructive = true,
            )
        }
    }
}
