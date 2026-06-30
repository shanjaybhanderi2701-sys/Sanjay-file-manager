package com.appblish.filora.feature.browser.dialog

import androidx.compose.runtime.Composable

/**
 * Dialog for renaming the item named [currentName] (FR-3.2). The field is
 * pre-filled with the current name (selected for quick replacement). [siblingNames]
 * should exclude [currentName] so re-confirming the same name is allowed; the live
 * validator rejects invalid characters and duplicates. [onConfirm] receives the
 * trimmed, validated name.
 */
@Composable
fun RenameDialog(
    currentName: String,
    siblingNames: Set<String>,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    submitError: String? = null,
) {
    NameInputDialog(
        title = "Rename",
        confirmLabel = "Rename",
        initialName = currentName,
        existingNames = siblingNames,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        submitError = submitError,
    )
}
