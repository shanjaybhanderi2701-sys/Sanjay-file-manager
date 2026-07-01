package com.appblish.filora.feature.storage

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appblish.filora.core.common.util.Formatters
import com.appblish.filora.core.domain.model.FileItem
import com.appblish.filora.core.ui.component.EmptyState

/**
 * Largest files (FR-8.2): the top space-consumers for a volume, largest-first. The
 * volume id arrives from navigation and is bound once ([LargestFilesViewModel.load]).
 * Loading shows a spinner, a scan error surfaces its message, and an empty result is
 * the "nothing big here" state. Delete/share entry points are a follow-up; this lands
 * the read-only surface the navigation graph and view-model already wire to.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LargestFilesScreen(
    volumeId: String?,
    modifier: Modifier = Modifier,
    viewModel: LargestFilesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(volumeId) { viewModel.load(volumeId) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.storage_largest_files_title)) })
        },
    ) { innerPadding ->
        when {
            uiState.isLoading ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }

            uiState.errorMessageRes != null ->
                EmptyState(
                    icon = Icons.Outlined.Folder,
                    title = stringResource(R.string.storage_largest_empty_title),
                    description = stringResource(uiState.errorMessageRes!!),
                    modifier = Modifier.padding(innerPadding),
                )

            uiState.files.isEmpty() ->
                EmptyState(
                    icon = Icons.Outlined.Folder,
                    title = stringResource(R.string.storage_largest_empty_title),
                    description = stringResource(R.string.storage_largest_empty_body),
                    modifier = Modifier.padding(innerPadding),
                )

            else ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    items(uiState.files, key = { it.path }) { file ->
                        LargestFileRow(file = file)
                    }
                }
        }
    }
}

@Composable
private fun LargestFileRow(file: FileItem) {
    ListItem(
        headlineContent = { Text(text = file.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Text(
                text = Formatters.formatSize(file.sizeBytes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        leadingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.InsertDriveFile,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
    )
}
