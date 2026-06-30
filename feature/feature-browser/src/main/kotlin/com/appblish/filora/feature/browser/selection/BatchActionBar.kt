package com.appblish.filora.feature.browser.selection

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Batch action bar shown while a multi-selection is active (FR-4.1). Renders the
 * selected count and a button per [BatchAction], each enabled only when the action
 * applies to the current selection ([SelectionState.enabledActions]). Stateless: the
 * caller (browser screen) owns the [MultiSelectController] and routes callbacks.
 */
@Composable
fun BatchActionBar(
    state: SelectionState,
    onClearSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onAction: (BatchAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClearSelection) {
                Icon(imageVector = Icons.Filled.Close, contentDescription = "Clear selection")
            }
            Text(
                text = "${state.count} selected",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp),
            )
            IconButton(onClick = onSelectAll) {
                Icon(imageVector = Icons.Filled.SelectAll, contentDescription = "Select all")
            }
            BatchAction.entries.forEach { action ->
                IconButton(
                    onClick = { onAction(action) },
                    enabled = action in state.enabledActions,
                ) {
                    Icon(imageVector = action.icon, contentDescription = action.label)
                }
            }
        }
    }
}

private val BatchAction.icon: ImageVector
    get() = when (this) {
        BatchAction.MOVE -> Icons.AutoMirrored.Filled.DriveFileMove
        BatchAction.COPY -> Icons.Filled.ContentCopy
        BatchAction.DELETE -> Icons.Filled.Delete
        BatchAction.SHARE -> Icons.Filled.Share
        BatchAction.ZIP -> Icons.Filled.FolderZip
    }
