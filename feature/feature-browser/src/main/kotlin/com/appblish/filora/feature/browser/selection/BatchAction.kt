package com.appblish.filora.feature.browser.selection

import androidx.annotation.StringRes
import com.appblish.filora.feature.browser.R

/**
 * Bulk operations offered by the batch action bar (FR-4.1). The enum is intentionally
 * free of any Compose types so the enablement rules stay pure-JVM testable;
 * [labelRes] is the string resource for the action's user-facing label, resolved at
 * the [BatchActionBar] composable. Icon mapping also lives next to that composable.
 */
enum class BatchAction(
    @StringRes val labelRes: Int
) {
    RENAME(R.string.browser_action_rename),
    MOVE(R.string.browser_action_move),
    COPY(R.string.browser_action_copy),
    DELETE(R.string.browser_action_delete),
    SHARE(R.string.browser_action_share),
    ZIP(R.string.browser_action_zip),
    ;

    companion object {
        /**
         * Resolves the actions enabled for the given [state]. An empty selection enables
         * nothing. [RENAME] targets exactly one entry, so it is offered only for a
         * single-item selection (FR-3.2). [SHARE] is withheld when the selection contains
         * a directory because `ACTION_SEND`/`ACTION_SEND_MULTIPLE` cannot target a folder;
         * every other action works on files and folders alike.
         */
        fun enabledFor(state: SelectionState): Set<BatchAction> {
            if (state.count == 0) return emptySet()
            val enabled = mutableSetOf(MOVE, COPY, DELETE, ZIP)
            if (state.count == 1) enabled += RENAME
            if (!state.containsDirectory) enabled += SHARE
            return enabled
        }
    }
}
