package com.appblish.filora.permission

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.appblish.filora.R

/**
 * Explicit justification surface for the all-files access opt-in (T022, FR-1.1).
 *
 * Play policy requires that `MANAGE_EXTERNAL_STORAGE` be requested only after the
 * user is told *why* and given a clear choice. This dialog states the justification
 * and routes [onConfirm] to the system "All files access" toggle
 * ([AllFilesAccess.manageAccessIntent]); [onDismiss] backs out leaving the
 * least-privilege media/SAF paths intact. Shown only on the gated `fullaccess`
 * build (see [AllFilesAccess.shouldOffer]).
 */
@Composable
fun FullAccessJustificationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.permission_full_access_title)) },
        text = { Text(stringResource(R.string.permission_full_access_body)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.permission_full_access_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.permission_dismiss))
            }
        },
    )
}
