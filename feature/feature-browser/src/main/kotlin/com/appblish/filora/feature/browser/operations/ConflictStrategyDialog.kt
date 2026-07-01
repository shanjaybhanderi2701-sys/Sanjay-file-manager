package com.appblish.filora.feature.browser.operations

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.appblish.filora.core.domain.model.ConflictStrategy
import com.appblish.filora.feature.browser.R

/**
 * Asks how to resolve destination name collisions before a copy/move runs (FR-3.3). The
 * chosen [ConflictStrategy] applies to the whole batch; the worker's [ConflictResolver]
 * applies it per item. Presented as three tappable options plus cancel.
 */
@Composable
fun ConflictStrategyDialog(
    onChoose: (ConflictStrategy) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.browser_conflict_title)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                ConflictOption(R.string.browser_conflict_keep_both) { onChoose(ConflictStrategy.KeepBoth) }
                ConflictOption(R.string.browser_conflict_replace) { onChoose(ConflictStrategy.Replace) }
                ConflictOption(R.string.browser_conflict_skip) { onChoose(ConflictStrategy.Skip) }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.browser_dialog_cancel)) }
        },
    )
}

@Composable
private fun ConflictOption(
    labelRes: Int,
    onClick: () -> Unit,
) {
    Text(
        text = stringResource(labelRes),
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp),
    )
}
