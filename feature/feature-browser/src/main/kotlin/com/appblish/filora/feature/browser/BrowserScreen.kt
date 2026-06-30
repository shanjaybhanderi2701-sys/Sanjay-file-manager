package com.appblish.filora.feature.browser

import android.content.Context
import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.foundation.lazy.grid.items as gridItems

/**
 * Browser screen (FR-2.1 listing; FR-2.3 sort; FR-2.4 show-hidden; FR-2.5 pull-to-refresh).
 * Tapping a folder navigates deeper via [onOpenDirectory]; the toolbar toggles list/grid,
 * opens the sort menu, and flips show-hidden — all persisted through the ViewModel.
 */
@Composable
fun BrowserScreen(
    location: String,
    modifier: Modifier = Modifier,
    onOpenDirectory: (String) -> Unit = {},
    viewModel: BrowserViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(location) { viewModel.bindLocation(location) }

    BrowserContent(
        uiState = uiState,
        onOpenItem = { item -> if (item.isDirectory) onOpenDirectory(item.path) },
        onToggleLayout = viewModel::toggleLayout,
        onSortBy = viewModel::setSortBy,
        onToggleHidden = { viewModel.setShowHidden(!uiState.showHidden) },
        onRefresh = viewModel::refresh,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BrowserContent(
    uiState: BrowserUiState,
    onOpenItem: (FileItem) -> Unit,
    onToggleLayout: () -> Unit,
    onSortBy: (SortOrder.By) -> Unit,
    onToggleHidden: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        BrowserToolbar(
            uiState = uiState,
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
                        BrowserGrid(uiState.entries, onOpenItem)
                    } else {
                        BrowserList(uiState.entries, onOpenItem)
                    }
            }
        }
    }
}

@Composable
private fun BrowserToolbar(
    uiState: BrowserUiState,
    onToggleLayout: () -> Unit,
    onSortBy: (SortOrder.By) -> Unit,
    onToggleHidden: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = uiState.location.substringAfterLast('/').ifBlank { uiState.location },
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f).padding(start = 8.dp),
            maxLines = 1,
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
    onOpenItem: (FileItem) -> Unit,
) {
    val context = LocalContext.current
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(entries, key = FileItem::path) { item ->
            FileRow(
                name = item.name,
                subtitle = item.subtitle(context),
                isDirectory = item.isDirectory,
                modifier = Modifier.clickable { onOpenItem(item) },
            )
        }
    }
}

@Composable
private fun BrowserGrid(
    entries: List<FileItem>,
    onOpenItem: (FileItem) -> Unit,
) {
    val context = LocalContext.current
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 112.dp),
        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
    ) {
        gridItems(entries, key = FileItem::path) { item ->
            GridTile(
                label = item.name,
                icon = fileIcon(item),
                caption = item.subtitle(context),
                modifier = Modifier.padding(4.dp).clickable { onOpenItem(item) },
            )
        }
    }
}

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
