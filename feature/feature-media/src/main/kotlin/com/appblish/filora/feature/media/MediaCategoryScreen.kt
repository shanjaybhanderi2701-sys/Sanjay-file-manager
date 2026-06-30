package com.appblish.filora.feature.media

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appblish.filora.core.domain.model.MediaCategory
import com.appblish.filora.core.ui.component.GridTile

/**
 * Media category hubs (FR-6.1): a grid of the seven first-class categories —
 * Images, Video, Audio, Docs, Downloads, APKs, Archives — each showing its item
 * count. Tapping a tile invokes [onOpenCategory] with the hub's [MediaCategory];
 * the category file list is wired in a later task.
 */
@Composable
fun MediaCategoryScreen(
    onOpenCategory: (MediaCategory) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MediaHubViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MediaCategoryContent(
        uiState = uiState,
        onOpenCategory = onOpenCategory,
        modifier = modifier,
    )
}

@Composable
internal fun MediaCategoryContent(
    uiState: MediaHubUiState,
    onOpenCategory: (MediaCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (uiState.isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 140.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(uiState.tiles, key = { it.hub.name }) { tile ->
            GridTile(
                label = tile.label,
                icon = tile.hub.icon,
                caption = tile.caption,
                modifier = Modifier.clickable { onOpenCategory(tile.hub.category) },
            )
        }
    }
}

/** Outlined icon for each hub. Kept in the Compose layer so [CategoryHub] stays pure. */
private val CategoryHub.icon: ImageVector
    get() =
        when (this) {
            CategoryHub.Images -> Icons.Outlined.Image
            CategoryHub.Video -> Icons.Outlined.Videocam
            CategoryHub.Audio -> Icons.Outlined.MusicNote
            CategoryHub.Docs -> Icons.Outlined.Description
            CategoryHub.Downloads -> Icons.Outlined.Download
            CategoryHub.Apks -> Icons.Outlined.Android
            CategoryHub.Archives -> Icons.Outlined.FolderZip
        }
