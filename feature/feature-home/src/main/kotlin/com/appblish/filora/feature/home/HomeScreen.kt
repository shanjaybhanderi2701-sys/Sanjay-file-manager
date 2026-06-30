package com.appblish.filora.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
                title = { Text("Filora") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
    ) { padding ->
        HomeContent(
            uiState = uiState,
            onOpenCategory = onOpenCategory,
            onBrowse = onBrowse,
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
) {
    when {
        uiState.isLoading ->
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

        uiState.permissionRequired ->
            EmptyState(
                icon = Icons.Outlined.Lock,
                title = "Grant storage access",
                description =
                    "Filora needs access to your media to show your library. " +
                        "Enable it in Settings, then return here.",
                modifier = modifier,
            )

        uiState.errorMessage != null ->
            EmptyState(
                icon = Icons.Outlined.FolderOff,
                title = uiState.errorMessage,
                modifier = modifier,
            )

        uiState.isEmpty ->
            EmptyState(
                icon = Icons.Outlined.FolderOpen,
                title = "Welcome to Filora",
                description = "Your files will appear here as you add them.",
                modifier = modifier,
            )

        else ->
            CategoryGrid(
                counts = uiState.categoryCounts,
                onOpenCategory = onOpenCategory,
                onBrowse = onBrowse,
                modifier = modifier,
            )
    }
}

@Composable
private fun CategoryGrid(
    counts: Map<MediaCategory, Int>,
    onOpenCategory: (MediaCategory) -> Unit,
    onBrowse: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 140.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(HomeCategory.entries.toList(), key = { it.name }) { hub ->
            val count = counts[hub.category]?.coerceAtLeast(0) ?: 0
            GridTile(
                label = hub.label,
                icon = hub.icon,
                caption = captionFor(count),
                modifier = Modifier.clickable { onOpenCategory(hub.category) },
            )
        }
        // Folder handoff to the M2 file browser, spanning its own row.
        item(span = { GridItemSpan(maxLineSpan) }) {
            GridTile(
                label = "Browse files",
                icon = Icons.Outlined.Folder,
                caption = "All folders",
                modifier = Modifier.clickable { onBrowse() },
            )
        }
    }
}

private fun captionFor(count: Int): String =
    when (count) {
        0 -> "Empty"
        1 -> "1 item"
        else -> "$count items"
    }

/**
 * The user-facing Home category tiles, in display order. [MediaCategory.Other] is a
 * classifier fallback, not a tile. Mirrors the Media hub's set (feature-media owns
 * its own copy; features can't depend on each other, so the small list is repeated).
 */
private enum class HomeCategory(
    val category: MediaCategory,
    val label: String,
    val icon: ImageVector,
) {
    Images(MediaCategory.Images, "Images", Icons.Outlined.Image),
    Video(MediaCategory.Video, "Video", Icons.Outlined.Videocam),
    Audio(MediaCategory.Audio, "Audio", Icons.Outlined.MusicNote),
    Docs(MediaCategory.Documents, "Docs", Icons.Outlined.Description),
    Downloads(MediaCategory.Downloads, "Downloads", Icons.Outlined.Download),
    Apks(MediaCategory.Apps, "APKs", Icons.Outlined.Android),
    Archives(MediaCategory.Archives, "Archives", Icons.Outlined.FolderZip),
}
