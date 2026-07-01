package com.appblish.filora.feature.browser.operations

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DriveFolderUpload
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.appblish.filora.core.domain.model.FileItem
import com.appblish.filora.feature.browser.R

/**
 * In-app folder chooser for a copy/move/zip destination on local storage (T069/T070,
 * FR-3.2). Lists the sub-folders of the current directory; tapping one descends, the
 * up control climbs (never above the picker's root), and the confirm button selects the
 * directory currently shown. The confirm label reflects the [kind] being performed.
 */
@Composable
fun DestinationPickerDialog(
    picker: FolderPickerState,
    kind: BatchOperationKind,
    onEnter: (FileItem) -> Unit,
    onUp: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.browser_dest_picker_title)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = picker.currentPath,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Box(modifier = Modifier.heightIn(min = 96.dp, max = 320.dp)) {
                    when {
                        picker.isLoading ->
                            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }

                        picker.errorMessageRes != null ->
                            Text(
                                text = stringResource(picker.errorMessageRes),
                                color = MaterialTheme.colorScheme.error,
                            )

                        else -> DestinationList(picker = picker, onEnter = onEnter, onUp = onUp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !picker.isLoading && picker.errorMessageRes == null) {
                Text(stringResource(confirmLabel(kind)))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.browser_dialog_cancel)) }
        },
    )
}

@Composable
private fun DestinationList(
    picker: FolderPickerState,
    onEnter: (FileItem) -> Unit,
    onUp: () -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        if (picker.canGoUp) {
            item {
                PickerRow(
                    icon = { Icon(Icons.Outlined.DriveFolderUpload, contentDescription = null) },
                    label = stringResource(R.string.browser_dest_picker_up),
                    onClick = onUp,
                )
            }
        }
        if (picker.directories.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.browser_dest_picker_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }
        }
        items(picker.directories, key = FileItem::path) { dir ->
            PickerRow(
                icon = { Icon(Icons.Outlined.Folder, contentDescription = null) },
                label = dir.name,
                onClick = { onEnter(dir) },
            )
        }
    }
}

@Composable
private fun PickerRow(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

private fun confirmLabel(kind: BatchOperationKind): Int =
    when (kind) {
        BatchOperationKind.COPY -> R.string.browser_dest_copy_here
        BatchOperationKind.MOVE -> R.string.browser_dest_move_here
        BatchOperationKind.ZIP -> R.string.browser_dest_zip_here
    }
