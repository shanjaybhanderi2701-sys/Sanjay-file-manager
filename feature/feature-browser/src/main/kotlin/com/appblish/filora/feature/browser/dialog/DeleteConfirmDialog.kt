package com.appblish.filora.feature.browser.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/**
 * Confirmation for single or batch delete (FR-3.4). Stateless: the caller derives the
 * [prompt] from the selection (count + whether it contains a folder + trash support)
 * via [DeleteConfirmation.forSelection] and runs [DeleteUseCase] from [onConfirm]. A
 * permanent delete colours the confirm button as an error to signal it can't be undone.
 */
@Composable
fun DeleteConfirmDialog(
    prompt: DeletePrompt,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(prompt.title) },
        text = { Text(prompt.message) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors =
                    if (prompt.destructive) {
                        ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        )
                    } else {
                        ButtonDefaults.textButtonColors()
                    },
            ) { Text(prompt.confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
