package com.appblish.filora.feature.browser.dialog

import androidx.compose.runtime.Composable

/**
 * Dialog for creating a new folder in the current directory (FR-3.1). [existingNames]
 * are the sibling names used for the live duplicate check; [submitError] surfaces a
 * conflict reported by the use case after submission. [onConfirm] receives the
 * trimmed, validated name.
 */
@Composable
fun CreateFolderDialog(
    existingNames: Set<String>,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    submitError: String? = null,
) {
    NameInputDialog(
        title = "New folder",
        confirmLabel = "Create",
        initialName = "",
        existingNames = existingNames,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        submitError = submitError,
    )
}
