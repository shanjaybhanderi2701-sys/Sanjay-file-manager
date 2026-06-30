package com.appblish.filora.feature.media

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appblish.filora.core.domain.model.FileItem
import com.appblish.filora.core.domain.model.MediaCategory
import com.appblish.filora.core.ui.component.EmptyState
import com.appblish.filora.core.ui.component.FileRow

/**
 * Detail list for one media [category] (FR-6.1). Streams the category's entries and,
 * on tap, opens the picked item in the system-associated app via [MediaOpenLauncher].
 * When no installed app can handle the type, a short toast tells the user instead of
 * failing silently.
 */
@Composable
fun MediaCategoryDetailScreen(
    category: MediaCategory,
    modifier: Modifier = Modifier,
    viewModel: MediaCategoryDetailViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(category) { viewModel.bind(category) }

    MediaCategoryDetailContent(
        uiState = uiState,
        onOpen = { item ->
            if (!MediaOpenLauncher.open(context, item)) {
                Toast.makeText(context, "No app can open this file", Toast.LENGTH_SHORT).show()
            }
        },
        modifier = modifier,
    )
}

@Composable
internal fun MediaCategoryDetailContent(
    uiState: MediaCategoryDetailUiState,
    onOpen: (FileItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        uiState.isLoading ->
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

        uiState.errorMessage != null ->
            EmptyState(
                icon = Icons.Outlined.SearchOff,
                title = uiState.errorMessage,
                modifier = modifier,
            )

        uiState.isEmpty ->
            EmptyState(
                icon = Icons.Outlined.FolderOff,
                title = "Nothing here yet",
                description = "Files in this category will show up here.",
                modifier = modifier,
            )

        else ->
            LazyColumn(modifier = modifier.fillMaxSize()) {
                items(uiState.items, key = { it.path }) { item ->
                    FileRow(
                        name = item.name,
                        subtitle = item.subtitle,
                        isDirectory = item.isDirectory,
                        modifier = Modifier.clickable { onOpen(item) },
                    )
                }
            }
    }
}

/** Compact "size · type" line under the file name; size is omitted for directories. */
private val FileItem.subtitle: String
    get() {
        val type = mimeType ?: extension.uppercase().ifEmpty { "File" }
        return if (isDirectory) type else "${formatSize(sizeBytes)} · $type"
    }

private fun formatSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val units = listOf("KB", "MB", "GB", "TB")
    var value = bytes.toDouble() / 1024
    var unit = 0
    while (value >= 1024 && unit < units.lastIndex) {
        value /= 1024
        unit++
    }
    return "${"%.1f".format(value)} ${units[unit]}"
}
