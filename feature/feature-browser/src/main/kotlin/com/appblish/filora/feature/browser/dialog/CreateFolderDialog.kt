package com.appblish.filora.feature.browser.dialog

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.appblish.filora.feature.browser.R

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
        title = stringResource(R.string.browser_dialog_new_folder_title),
        confirmLabel = stringResource(R.string.browser_dialog_create_confirm),
        initialName = "",
        existingNames = existingNames,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        submitError = submitError,
    )
}
