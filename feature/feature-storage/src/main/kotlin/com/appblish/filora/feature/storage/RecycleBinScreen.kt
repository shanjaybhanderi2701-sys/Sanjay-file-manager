package com.appblish.filora.feature.storage

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appblish.filora.core.common.util.Formatters
import com.appblish.filora.core.domain.model.TrashedItem
import com.appblish.filora.core.ui.component.EmptyState

/**
 * Recycle Bin (FR-3.4, T125/T129): the app-managed trash listed newest-first, each item
 * offering restore-to-original or permanent delete. The app bar shows the bin's total
 * footprint and an empty-trash action; both destructive paths confirm first. Items and
 * size stream from the trash, so a restore/delete updates the list without a manual
 * refresh, and expired items are auto-purged when the screen opens (T128).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecycleBinScreen(
    modifier: Modifier = Modifier,
    onNavigateUp: () -> Unit = {},
    viewModel: RecycleBinViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var confirmEmpty by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<TrashedItem?>(null) }

    val message = uiState.message
    val messageText = message?.let { stringResource(it.res) }
    LaunchedEffect(message) {
        if (messageText != null) {
            snackbarHostState.showSnackbar(messageText)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.recycle_bin_title))
                        if (uiState.items.isNotEmpty()) {
                            Text(
                                text = stringResource(
                                    R.string.recycle_bin_size_summary,
                                    Formatters.formatSize(uiState.totalSizeBytes),
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    if (uiState.items.isNotEmpty()) {
                        IconButton(onClick = { confirmEmpty = true }) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteForever,
                                contentDescription = stringResource(R.string.recycle_bin_empty),
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            uiState.isEmpty ->
                EmptyState(
                    icon = Icons.Outlined.DeleteOutline,
                    title = stringResource(R.string.recycle_bin_empty_title),
                    description = stringResource(R.string.recycle_bin_empty_body),
                    modifier = Modifier.padding(innerPadding),
                )

            else ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    items(uiState.items, key = { it.id }) { item ->
                        TrashRow(
                            item = item,
                            onRestore = { viewModel.restore(item.id) },
                            onDelete = { pendingDelete = item },
                        )
                    }
                }
        }
    }

    if (confirmEmpty) {
        ConfirmDialog(
            title = stringResource(R.string.recycle_bin_empty_dialog_title),
            body = stringResource(R.string.recycle_bin_empty_dialog_body),
            confirmLabel = stringResource(R.string.recycle_bin_empty),
            onConfirm = {
                confirmEmpty = false
                viewModel.emptyBin()
            },
            onDismiss = { confirmEmpty = false },
        )
    }

    pendingDelete?.let { target ->
        ConfirmDialog(
            title = stringResource(R.string.recycle_bin_delete_dialog_title),
            body = stringResource(R.string.recycle_bin_delete_dialog_body, target.name),
            confirmLabel = stringResource(R.string.recycle_bin_action_delete),
            onConfirm = {
                pendingDelete = null
                viewModel.deleteForever(target.id)
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun TrashRow(
    item: TrashedItem,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(text = item.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Text(
                text = stringResource(
                    R.string.recycle_bin_item_meta,
                    Formatters.formatSize(item.sizeBytes),
                    Formatters.formatDate(item.deletedAtEpochMillis),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingContent = {
            Icon(
                imageVector = if (item.isDirectory) {
                    Icons.Outlined.Folder
                } else {
                    Icons.AutoMirrored.Outlined.InsertDriveFile
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        trailingContent = { TrashRowMenu(onRestore = onRestore, onDelete = onDelete) },
    )
}

@Composable
private fun TrashRowMenu(
    onRestore: () -> Unit,
    onDelete: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }) {
        Icon(
            imageVector = Icons.Outlined.MoreVert,
            contentDescription = stringResource(R.string.recycle_bin_item_actions),
        )
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.recycle_bin_action_restore)) },
            leadingIcon = { Icon(Icons.Outlined.Restore, contentDescription = null) },
            onClick = {
                expanded = false
                onRestore()
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.recycle_bin_action_delete)) },
            leadingIcon = { Icon(Icons.Outlined.DeleteForever, contentDescription = null) },
            onClick = {
                expanded = false
                onDelete()
            },
        )
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.DeleteForever, contentDescription = null) },
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.recycle_bin_action_cancel)) }
        },
    )
}
