package com.appblish.filora.feature.browser.selection

/**
 * Bulk operations offered by the batch action bar (FR-4.1). The enum is intentionally
 * free of any Compose/Android types so the enablement rules stay pure-JVM testable;
 * icon mapping lives next to the [BatchActionBar] composable.
 */
enum class BatchAction(
    val label: String
) {
    MOVE("Move"),
    COPY("Copy"),
    DELETE("Delete"),
    SHARE("Share"),
    ZIP("Zip"),
    ;

    companion object {
        /**
         * Resolves the actions enabled for the given [state]. An empty selection enables
         * nothing. [SHARE] is withheld when the selection contains a directory because
         * `ACTION_SEND`/`ACTION_SEND_MULTIPLE` cannot target a folder; every other action
         * works on files and folders alike.
         */
        fun enabledFor(state: SelectionState): Set<BatchAction> {
            if (state.count == 0) return emptySet()
            val enabled = mutableSetOf(MOVE, COPY, DELETE, ZIP)
            if (!state.containsDirectory) enabled += SHARE
            return enabled
        }
    }
}
