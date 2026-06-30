package com.appblish.filora.feature.home

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appblish.filora.core.domain.model.FileItem
import com.appblish.filora.core.domain.model.MediaCategory
import com.appblish.filora.core.ui.component.EmptyState
import com.appblish.filora.core.ui.component.GridTile

/**
 * Home dashboard (M4 T4.6). Shows the media category counts as a grid of tiles,
 * with permission-aware empty states:
 *
 * - no media access → a "grant access" prompt ([onOpenSettings] leads to settings);
 * - access but nothing indexed yet → a neutral empty state;
 * - access with counts → the seven category tiles (tap hands off to the category
 *   list via [onOpenCategory]) plus a "Browse files" tile that hands off to the
 *   file browser (M2) via [onBrowse].
 *
 * Counts are re-queried on every `onResume` so a grant the user toggled in system
 * settings, or files added while backgrounded, are reflected on return.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenCategory: (MediaCategory) -> Unit = {},
    onBrowse: () -> Unit = {},
    onOpenItem: (FileItem) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Permission-aware refresh: re-query counts each time Home returns to the
    // foreground (e.g. back from the system permission settings or a file op).
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose {}
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_title)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.home_action_settings),
                        )
                    }
                },
            )
        },
    ) { padding ->
        HomeContent(
            uiState = uiState,
            onOpenCategory = onOpenCategory,
            onBrowse = onBrowse,
            onOpenItem = onOpenItem,
            modifier = Modifier.padding(padding),
        )
    }
}

@Composable
internal fun HomeContent(
    uiState: HomeUiState,
    onOpenCategory: (MediaCategory) -> Unit,
    onBrowse: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenItem: (FileItem) -> Unit = {},
) {
    when {
        uiState.isLoading ->
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

        uiState.permissionRequired ->
            EmptyState(
                icon = Icons.Outlined.Lock,
                title = stringResource(R.string.home_permission_title),
                description = stringResource(R.string.home_permission_body),
                modifier = modifier,
            )

        uiState.errorMessageRes != null ->
            EmptyState(
                icon = Icons.Outlined.FolderOff,
                title = stringResource(uiState.errorMessageRes),
                modifier = modifier,
            )

        uiState.isEmpty ->
            EmptyState(
                icon = Icons.Outlined.FolderOpen,
                title = stringResource(R.string.home_welcome_title),
                description = stringResource(R.string.home_welcome_body),
                modifier = modifier,
            )

        else ->
            HomeDashboard(
                uiState = uiState,
                onOpenCategory = onOpenCategory,
                onBrowse = onBrowse,
                onOpenItem = onOpenItem,
                modifier = modifier,
            )
    }
}

/**
 * The granted-access dashboard: the user's Recents (FR-9.2) and Favorites (FR-9.1)
 * as horizontally scrolling rows, above the seven category tiles and the file-browser
 * handoff. Recents/favorites sections are omitted when empty so a fresh install shows
 * just the categories. Everything lives in one [LazyVerticalGrid] — the file rows are
 * full-span items so they span the adaptive columns.
 */
@Composable
private fun HomeDashboard(
    uiState: HomeUiState,
    onOpenCategory: (MediaCategory) -> Unit,
    onBrowse: () -> Unit,
    onOpenItem: (FileItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val recentTitle = stringResource(R.string.home_section_recent)
    val favoritesTitle = stringResource(R.string.home_section_favorites)
    val folderCaption = stringResource(R.string.home_chip_folder)
    val fileCaption = stringResource(R.string.home_chip_file)
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 140.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (uiState.recents.isNotEmpty()) {
            sectionHeader(recentTitle, Icons.Outlined.History)
            fileChipRow(uiState.recents, folderCaption, fileCaption, onOpenItem)
        }
        if (uiState.favorites.isNotEmpty()) {
            sectionHeader(favoritesTitle, Icons.Outlined.Star)
            fileChipRow(uiState.favorites, folderCaption, fileCaption, onOpenItem)
        }

        items(HomeCategory.entries.toList(), key = { it.name }) { hub ->
            val count = uiState.categoryCounts[hub.category]?.coerceAtLeast(0) ?: 0
            GridTile(
                label = stringResource(hub.labelRes),
                icon = hub.icon,
                caption = captionFor(count),
                modifier = Modifier.clickable { onOpenCategory(hub.category) },
            )
        }
        // Folder handoff to the M2 file browser, spanning its own row.
        item(span = { GridItemSpan(maxLineSpan) }) {
            GridTile(
                label = stringResource(R.string.home_browse_files),
                icon = Icons.Outlined.Folder,
                caption = stringResource(R.string.home_browse_caption),
                modifier = Modifier.clickable { onBrowse() },
            )
        }
    }
}

/** A full-span section label with a leading icon. */
private fun LazyGridScope.sectionHeader(
    title: String,
    icon: ImageVector,
) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(imageVector = icon, contentDescription = null)
            Text(text = title, style = MaterialTheme.typography.titleMedium)
        }
    }
}

/** A full-span, horizontally scrolling strip of file/folder chips. */
private fun LazyGridScope.fileChipRow(
    items: List<FileItem>,
    folderCaption: String,
    fileCaption: String,
    onOpenItem: (FileItem) -> Unit,
) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items.forEach { item ->
                GridTile(
                    label = item.name,
                    icon = if (item.isDirectory) Icons.Outlined.Folder else Icons.Outlined.InsertDriveFile,
                    caption = if (item.isDirectory) folderCaption else fileCaption,
                    modifier =
                        Modifier
                            .width(120.dp)
                            .clickable { onOpenItem(item) },
                )
            }
        }
    }
}

@Composable
private fun captionFor(count: Int): String =
    if (count == 0) {
        stringResource(R.string.home_caption_empty)
    } else {
        pluralStringResource(R.plurals.home_item_count, count, count)
    }

/**
 * The user-facing Home category tiles, in display order. [MediaCategory.Other] is a
 * classifier fallback, not a tile. Mirrors the Media hub's set (feature-media owns
 * its own copy; features can't depend on each other, so the small list is repeated).
 */
private enum class HomeCategory(
    val category: MediaCategory,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    Images(MediaCategory.Images, R.string.home_cat_images, Icons.Outlined.Image),
    Video(MediaCategory.Video, R.string.home_cat_video, Icons.Outlined.Videocam),
    Audio(MediaCategory.Audio, R.string.home_cat_audio, Icons.Outlined.MusicNote),
    Docs(MediaCategory.Documents, R.string.home_cat_docs, Icons.Outlined.Description),
    Downloads(MediaCategory.Downloads, R.string.home_cat_downloads, Icons.Outlined.Download),
    Apks(MediaCategory.Apps, R.string.home_cat_apks, Icons.Outlined.Android),
    Archives(MediaCategory.Archives, R.string.home_cat_archives, Icons.Outlined.FolderZip),
}
