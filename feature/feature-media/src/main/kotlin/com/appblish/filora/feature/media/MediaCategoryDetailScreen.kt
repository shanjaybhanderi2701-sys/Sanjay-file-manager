package com.appblish.filora.feature.media

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appblish.filora.core.domain.model.FileItem
import com.appblish.filora.core.domain.model.MediaCategory
import com.appblish.filora.core.ui.component.EmptyState
import com.appblish.filora.core.ui.component.FileRow
import kotlinx.coroutines.launch

/**
 * Detail list for one media [category] (FR-6.1). Streams the category's entries and,
 * on tap, opens the picked item in the system-associated app via [MediaIntents]; a
 * long-press shares it through the system share sheet (FR-10). When no installed app
 * can handle the action, a snackbar tells the user instead of failing silently.
 */
@Composable
fun MediaCategoryDetailScreen(
    category: MediaCategory,
    modifier: Modifier = Modifier,
    viewModel: MediaCategoryDetailViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(category) { viewModel.bind(category) }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        MediaCategoryDetailContent(
            uiState = uiState,
            onOpen = { item ->
                if (!MediaIntents.open(context, item)) {
                    scope.launch {
                        snackbarHostState.showSnackbar(context.getString(R.string.media_open_failed))
                    }
                }
            },
            onShare = { item ->
                if (!MediaIntents.share(context, item)) {
                    scope.launch {
                        snackbarHostState.showSnackbar(context.getString(R.string.media_share_failed))
                    }
                }
            },
            modifier = Modifier.padding(padding),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun MediaCategoryDetailContent(
    uiState: MediaCategoryDetailUiState,
    onOpen: (FileItem) -> Unit,
    onShare: (FileItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        uiState.isLoading ->
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

        uiState.errorMessageRes != null ->
            EmptyState(
                icon = Icons.Outlined.SearchOff,
                title = stringResource(uiState.errorMessageRes),
                modifier = modifier,
            )

        uiState.isEmpty ->
            EmptyState(
                icon = Icons.Outlined.FolderOff,
                title = stringResource(R.string.media_empty_title),
                description = stringResource(R.string.media_empty_body),
                modifier = modifier,
            )

        else ->
            LazyColumn(modifier = modifier.fillMaxSize()) {
                items(uiState.items, key = { it.path }) { item ->
                    val unknownType = stringResource(R.string.media_type_file)
                    FileRow(
                        name = item.name,
                        subtitle = item.subtitle(unknownType),
                        isDirectory = item.isDirectory,
                        modifier =
                            Modifier.combinedClickable(
                                onClick = { onOpen(item) },
                                onLongClick = { onShare(item) },
                            ),
                    )
                }
            }
    }
}

/**
 * Compact "size · type" line under the file name; size is omitted for directories.
 * [unknownType] is the localized fallback label used when neither a MIME type nor an
 * extension is available.
 */
private fun FileItem.subtitle(unknownType: String): String {
    val type = mimeType ?: extension.uppercase().ifEmpty { unknownType }
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
