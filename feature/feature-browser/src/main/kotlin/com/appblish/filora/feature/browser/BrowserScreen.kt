package com.appblish.filora.feature.browser

import android.content.Context
import android.content.Intent
import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appblish.filora.core.domain.model.FileItem
import com.appblish.filora.core.domain.model.SortOrder
import com.appblish.filora.core.domain.model.ViewLayout
import com.appblish.filora.core.ui.component.EmptyState
import com.appblish.filora.core.ui.component.FileRow
import com.appblish.filora.core.ui.component.GridTile
import com.appblish.filora.feature.browser.dialog.CreateFolderDialog
import com.appblish.filora.feature.browser.dialog.DeleteConfirmDialog
import com.appblish.filora.feature.browser.dialog.DeleteConfirmation
import com.appblish.filora.feature.browser.dialog.RenameDialog
import com.appblish.filora.feature.browser.operations.BatchOperationKind
import com.appblish.filora.feature.browser.operations.ConflictStrategyDialog
import com.appblish.filora.feature.browser.operations.DestinationPickerDialog
import com.appblish.filora.feature.browser.operations.OperationFlowState
import com.appblish.filora.feature.browser.operations.OperationProgressSheet
import com.appblish.filora.feature.browser.selection.BatchAction
import com.appblish.filora.feature.browser.selection.BatchActionBar
import com.appblish.filora.feature.browser.selection.SelectionState
import com.appblish.filora.feature.browser.share.FileShareLauncher
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.grid.items as gridItems

/**
 * Browser screen (FR-2.1 listing; FR-2.3 sort; FR-2.4 show-hidden; FR-2.5 pull-to-refresh;
 * FR-3.1/3.2/3.4 create/rename/delete; FR-4.1 multi-select + batch actions; FR-9.1 favorites;
 * FR-10.2 share).
 *
 * A normal tap opens a folder ([onOpenDirectory]) or a file (system open); while a
 * multi-selection is active a tap toggles instead. A long-press anchors a context menu
 * offering "Select" (enter multi-select, T075) and pin/unpin (FR-9.1, T094). The batch
 * action bar drives rename/delete/share; the new-folder FAB and the create/rename/delete
 * dialogs route through the ViewModel, which mutates the list in place (T077).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod") // Top-level screen scaffold: FAB + batch bar + content + dialog/picker/progress overlays.
@Composable
fun BrowserScreen(
    location: String,
    modifier: Modifier = Modifier,
    onOpenDirectory: (String) -> Unit = {},
    viewModel: BrowserViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(location) { viewModel.bindLocation(location) }

    // One-shot operation messages — show then acknowledge (T079 result/error reporting).
    val messageRes = uiState.messageRes
    if (messageRes != null) {
        val message = stringResource(messageRes)
        LaunchedEffect(messageRes, message) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    // Copy/move over a SAF tree defers to the system folder picker; take a persistable
    // read/write grant on the chosen tree so the worker can write into it (FR-3.2).
    val safDestinationPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { treeUri ->
            if (treeUri != null) {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(treeUri, flags)
                viewModel.onSafDestinationPicked(treeUri.toString())
            } else {
                viewModel.cancelOperationFlow()
            }
        }
    val safRequest = (uiState.operationFlow as? OperationFlowState.PickingSafDestination)?.request
    LaunchedEffect(safRequest) {
        if (safRequest != null) safDestinationPicker.launch(null)
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (!uiState.inSelectionMode && !uiState.isLoading && !uiState.isError) {
                FloatingActionButton(onClick = viewModel::showNewFolderDialog) {
                    Icon(
                        imageVector = Icons.Outlined.CreateNewFolder,
                        contentDescription = stringResource(R.string.browser_action_new_folder),
                    )
                }
            }
        },
        bottomBar = {
            if (uiState.inSelectionMode) {
                BatchActionBar(
                    state = uiState.selection,
                    onClearSelection = viewModel::clearSelection,
                    onSelectAll = viewModel::selectAll,
                    onAction = { action ->
                        onBatchAction(
                            action = action,
                            uiState = uiState,
                            viewModel = viewModel,
                            context = context,
                            launch = { block -> scope.launch { block() } },
                        )
                    },
                )
            }
        },
    ) { padding ->
        BrowserContent(
            uiState = uiState,
            onNavigate = onOpenDirectory,
            onItemTap = { item ->
                when {
                    uiState.inSelectionMode -> viewModel.toggleSelection(item)
                    item.isDirectory -> onOpenDirectory(item.path)
                    else ->
                        scope.launch {
                            if (!FileShareLauncher.openFile(context, item)) {
                                viewModel.showMessage(R.string.browser_open_no_app)
                            }
                        }
                }
            },
            onToggleSelection = viewModel::toggleSelection,
            onToggleFavorite = viewModel::toggleFavorite,
            onToggleLayout = viewModel::toggleLayout,
            onSortBy = viewModel::setSortBy,
            onToggleHidden = { viewModel.setShowHidden(!uiState.showHidden) },
            onRefresh = viewModel::refresh,
            modifier = Modifier.padding(padding),
        )
    }

    BrowserDialogs(
        dialog = uiState.dialog,
        uiState = uiState,
        viewModel = viewModel,
    )

    BrowserOperationOverlays(uiState = uiState, viewModel = viewModel)
}

/**
 * The copy/move/zip overlays: the in-app destination folder chooser and conflict-strategy
 * prompt (T069/T070, FR-3.2/3.3) and the live worker-progress sheet with cancel (T079).
 * The SAF destination stage owns no UI here — the system picker is launched from a
 * [LaunchedEffect] in [BrowserScreen].
 */
@Composable
private fun BrowserOperationOverlays(
    uiState: BrowserUiState,
    viewModel: BrowserViewModel,
) {
    when (val flow = uiState.operationFlow) {
        is OperationFlowState.PickingLocalDestination ->
            DestinationPickerDialog(
                picker = flow.picker,
                kind = flow.request.kind,
                onEnter = viewModel::pickerEnter,
                onUp = viewModel::pickerUp,
                onConfirm = viewModel::pickerConfirm,
                onDismiss = viewModel::cancelOperationFlow,
            )

        is OperationFlowState.ChoosingConflict ->
            ConflictStrategyDialog(
                onChoose = viewModel::chooseConflict,
                onDismiss = viewModel::cancelOperationFlow,
            )

        is OperationFlowState.PickingSafDestination, null -> Unit
    }

    uiState.activeOperation?.let { operation ->
        OperationProgressSheet(
            operation = operation,
            onCancel = viewModel::cancelActiveOperation,
        )
    }
}

/** Routes a batch action to the ViewModel or the system share sheet (T076/T078). */
private fun onBatchAction(
    action: BatchAction,
    uiState: BrowserUiState,
    viewModel: BrowserViewModel,
    context: Context,
    launch: (suspend () -> Unit) -> Unit,
) {
    when (action) {
        BatchAction.RENAME -> uiState.selectedItems().singleOrNull()?.let(viewModel::showRenameDialog)
        BatchAction.DELETE -> viewModel.showDeleteDialog()
        BatchAction.SHARE ->
            launch {
                val shared = FileShareLauncher.shareFiles(context, uiState.selectedItems())
                if (!shared) viewModel.showMessage(R.string.browser_share_no_app)
            }
        // Copy/move/zip open a destination picker, then enqueue with a live progress sheet (T069/T070/T079).
        BatchAction.MOVE -> viewModel.beginBatchOperation(BatchOperationKind.MOVE)
        BatchAction.COPY -> viewModel.beginBatchOperation(BatchOperationKind.COPY)
        BatchAction.ZIP -> viewModel.beginBatchOperation(BatchOperationKind.ZIP)
    }
}

/** The create/rename/delete dialog overlay (T066–T068). */
@Composable
private fun BrowserDialogs(
    dialog: BrowserDialog?,
    uiState: BrowserUiState,
    viewModel: BrowserViewModel,
) {
    when (dialog) {
        BrowserDialog.NewFolder ->
            CreateFolderDialog(
                existingNames = uiState.entries.map(FileItem::name).toSet(),
                onConfirm = viewModel::confirmCreateFolder,
                onDismiss = viewModel::dismissDialog,
            )

        is BrowserDialog.Rename ->
            RenameDialog(
                currentName = dialog.currentName,
                siblingNames = uiState.entries
                    .filterNot { it.path == dialog.path }
                    .map(FileItem::name)
                    .toSet(),
                onConfirm = viewModel::confirmRename,
                onDismiss = viewModel::dismissDialog,
            )

        BrowserDialog.ConfirmDelete ->
            DeleteConfirmDialog(
                prompt =
                    DeleteConfirmation.forSelection(
                        count = uiState.selection.count,
                        containsDirectory = uiState.selection.containsDirectory,
                        toTrash = true,
                    ),
                onConfirm = viewModel::confirmDelete,
                onDismiss = viewModel::dismissDialog,
            )

        null -> Unit
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BrowserContent(
    uiState: BrowserUiState,
    onItemTap: (FileItem) -> Unit,
    onToggleSelection: (FileItem) -> Unit,
    onToggleFavorite: (FileItem) -> Unit,
    onToggleLayout: () -> Unit,
    onSortBy: (SortOrder.By) -> Unit,
    onToggleHidden: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigate: (String) -> Unit = {},
) {
    Column(modifier = modifier.fillMaxSize()) {
        BrowserToolbar(
            uiState = uiState,
            onNavigate = onNavigate,
            onToggleLayout = onToggleLayout,
            onSortBy = onSortBy,
            onToggleHidden = onToggleHidden,
        )
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            when {
                uiState.isLoading ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }

                uiState.isError ->
                    EmptyState(
                        icon = Icons.Outlined.WarningAmber,
                        title = stringResource(R.string.browser_error_title),
                        description = uiState.errorMessageRes?.let { stringResource(it) },
                        modifier = Modifier.fillMaxSize(),
                    )

                uiState.isEmpty ->
                    EmptyState(
                        icon = Icons.Outlined.Folder,
                        title = stringResource(R.string.browser_empty_title),
                        description = stringResource(R.string.browser_empty_description),
                        modifier = Modifier.fillMaxSize(),
                    )

                else ->
                    if (uiState.layout == ViewLayout.Grid) {
                        BrowserGrid(
                            uiState.entries,
                            uiState.selection,
                            uiState.favoritePaths,
                            onItemTap,
                            onToggleSelection,
                            onToggleFavorite
                        )
                    } else {
                        BrowserList(
                            uiState.entries,
                            uiState.selection,
                            uiState.favoritePaths,
                            onItemTap,
                            onToggleSelection,
                            onToggleFavorite
                        )
                    }
            }
        }
    }
}

@Composable
private fun BrowserToolbar(
    uiState: BrowserUiState,
    onNavigate: (String) -> Unit,
    onToggleLayout: () -> Unit,
    onSortBy: (SortOrder.By) -> Unit,
    onToggleHidden: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BreadcrumbBar(
            location = uiState.location,
            onNavigate = onNavigate,
            modifier = Modifier.weight(1f).padding(start = 8.dp),
        )
        IconButton(onClick = onToggleHidden) {
            Icon(
                imageVector = if (uiState.showHidden) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                contentDescription =
                    stringResource(
                        if (uiState.showHidden) {
                            R.string.browser_action_hide_hidden
                        } else {
                            R.string.browser_action_show_hidden
                        },
                    ),
            )
        }
        SortMenu(sortOrder = uiState.sortOrder, onSortBy = onSortBy)
        IconButton(onClick = onToggleLayout) {
            Icon(
                imageVector =
                    if (uiState.layout == ViewLayout.Grid) {
                        Icons.AutoMirrored.Outlined.ViewList
                    } else {
                        Icons.Outlined.GridView
                    },
                contentDescription =
                    stringResource(
                        if (uiState.layout == ViewLayout.Grid) {
                            R.string.browser_action_layout_list
                        } else {
                            R.string.browser_action_layout_grid
                        },
                    ),
            )
        }
    }
}

@Composable
private fun SortMenu(
    sortOrder: SortOrder,
    onSortBy: (SortOrder.By) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }) {
        Icon(Icons.Outlined.Sort, contentDescription = stringResource(R.string.browser_action_sort))
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        SortOrder.By.entries.forEach { by ->
            val active = sortOrder.by == by
            DropdownMenuItem(
                text = { Text(stringResource(sortLabel(by))) },
                onClick = {
                    onSortBy(by)
                    expanded = false
                },
                trailingIcon = {
                    if (active) Text(if (sortOrder.ascending) "↑" else "↓")
                },
            )
        }
    }
}

@Composable
private fun BrowserList(
    entries: List<FileItem>,
    selection: SelectionState,
    favoritePaths: Set<String>,
    onItemTap: (FileItem) -> Unit,
    onToggleSelection: (FileItem) -> Unit,
    onToggleFavorite: (FileItem) -> Unit,
) {
    val context = LocalContext.current
    // Stable per-path keys let Compose reuse row nodes across scroll (NFR-1.3 fps); the
    // testTag gives the macrobenchmark a deterministic scroll anchor over the 10k fixture.
    LazyColumn(modifier = Modifier.fillMaxSize().testTag(BrowserTestTags.LIST)) {
        items(entries, key = FileItem::path) { item ->
            FileEntryContextMenu(
                item = item,
                isFavorite = item.path in favoritePaths,
                onItemTap = onItemTap,
                onToggleSelection = onToggleSelection,
                onToggleFavorite = onToggleFavorite,
            ) { onTap, onLongPress ->
                FileRow(
                    name = item.name,
                    subtitle = item.subtitle(context),
                    isDirectory = item.isDirectory,
                    selected = selection.isSelected(item.path),
                    modifier = rowGestures(onTap, onLongPress),
                )
            }
        }
    }
}

@Composable
private fun BrowserGrid(
    entries: List<FileItem>,
    selection: SelectionState,
    favoritePaths: Set<String>,
    onItemTap: (FileItem) -> Unit,
    onToggleSelection: (FileItem) -> Unit,
    onToggleFavorite: (FileItem) -> Unit,
) {
    val context = LocalContext.current
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 112.dp),
        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp).testTag(BrowserTestTags.GRID),
    ) {
        gridItems(entries, key = FileItem::path) { item ->
            val selected = selection.isSelected(item.path)
            FileEntryContextMenu(
                item = item,
                isFavorite = item.path in favoritePaths,
                onItemTap = onItemTap,
                onToggleSelection = onToggleSelection,
                onToggleFavorite = onToggleFavorite,
            ) { onTap, onLongPress ->
                GridTile(
                    label = item.name,
                    icon = fileIcon(item),
                    caption = item.subtitle(context),
                    modifier =
                        Modifier
                            .padding(4.dp)
                            .then(rowGestures(onTap, onLongPress))
                            .then(
                                if (selected) {
                                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                                } else {
                                    Modifier
                                },
                            ),
                )
            }
        }
    }
}

/**
 * Wraps a list/grid entry with a long-press context menu (FR-4.1, FR-9.1). A tap routes
 * to [onItemTap] (open, or toggle while selecting); a long-press anchors a [DropdownMenu]
 * over the entry offering "Select" — which enters multi-select via [onToggleSelection]
 * (T075) — and pin/unpin via [onToggleFavorite] (T094), the star icon and label
 * reflecting [isFavorite]. [content] receives the tap and long-press callbacks so the
 * caller can attach them to either a [FileRow] or a [GridTile].
 */
@Composable
private fun FileEntryContextMenu(
    item: FileItem,
    isFavorite: Boolean,
    onItemTap: (FileItem) -> Unit,
    onToggleSelection: (FileItem) -> Unit,
    onToggleFavorite: (FileItem) -> Unit,
    content: @Composable (onTap: () -> Unit, onLongPress: () -> Unit) -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Box {
        content({ onItemTap(item) }, { menuExpanded = true })
        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.browser_action_select)) },
                leadingIcon = {
                    Icon(imageVector = Icons.Outlined.Checklist, contentDescription = null)
                },
                onClick = {
                    onToggleSelection(item)
                    menuExpanded = false
                },
            )
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(
                            if (isFavorite) {
                                R.string.browser_action_unpin
                            } else {
                                R.string.browser_action_pin
                            },
                        ),
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (isFavorite) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                        contentDescription = null,
                    )
                },
                onClick = {
                    onToggleFavorite(item)
                    menuExpanded = false
                },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun rowGestures(
    onTap: () -> Unit,
    onLongPress: () -> Unit,
): Modifier = Modifier.combinedClickable(onClick = onTap, onLongClick = onLongPress)

private fun sortLabel(by: SortOrder.By): Int =
    when (by) {
        SortOrder.By.Name -> R.string.browser_sort_name
        SortOrder.By.Size -> R.string.browser_sort_size
        SortOrder.By.DateModified -> R.string.browser_sort_date
        SortOrder.By.Type -> R.string.browser_sort_type
    }

/** "Folder" for directories, else a "<size> · <relative date>" line. */
private fun FileItem.subtitle(context: Context): String =
    if (isDirectory) {
        context.getString(R.string.browser_subtitle_folder)
    } else {
        val size = Formatter.formatShortFileSize(context, sizeBytes)
        val date =
            DateUtils.getRelativeTimeSpanString(
                lastModifiedEpochMillis,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
            )
        "$size · $date"
    }
