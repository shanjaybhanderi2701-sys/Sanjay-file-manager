package com.appblish.filora.feature.storage

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appblish.filora.core.common.util.Formatters
import com.appblish.filora.core.domain.model.CategoryUsage
import com.appblish.filora.core.domain.model.MediaCategory
import com.appblish.filora.core.domain.model.StorageVolume
import com.appblish.filora.core.domain.model.VolumeBreakdown
import com.appblish.filora.core.ui.component.EmptyState

/**
 * Storage breakdown screen (FR-8.1): one card per volume showing used/free plus a
 * by-category usage list. Tapping a category invokes [onOpenCategory] with the
 * owning volume so the caller can open that category's hub filtered to the volume.
 * Removable volumes show used/free only (their content isn't in the media index).
 */
@Composable
fun StorageScreen(
    modifier: Modifier = Modifier,
    onOpenCategory: (StorageVolume, MediaCategory) -> Unit = { _, _ -> },
    onOpenLargestFiles: () -> Unit = {},
    viewModel: StorageViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    StorageContent(
        uiState = uiState,
        onOpenCategory = onOpenCategory,
        onOpenLargestFiles = onOpenLargestFiles,
        modifier = modifier,
    )
}

@Composable
internal fun StorageContent(
    uiState: StorageUiState,
    onOpenCategory: (StorageVolume, MediaCategory) -> Unit,
    onOpenLargestFiles: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val volumes = uiState.breakdown?.volumes.orEmpty()
    when {
        uiState.isLoading ->
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

        uiState.errorMessageRes != null ->
            EmptyState(
                icon = Icons.Outlined.Storage,
                title = stringResource(R.string.storage_title),
                description = stringResource(uiState.errorMessageRes),
                modifier = modifier,
            )

        volumes.isEmpty() ->
            EmptyState(
                icon = Icons.Outlined.Storage,
                title = stringResource(R.string.storage_empty_title),
                description = stringResource(R.string.storage_empty_body),
                modifier = modifier,
            )

        else ->
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item(key = "largest-files") {
                    LargestFilesCard(onClick = onOpenLargestFiles)
                }
                items(volumes, key = { it.volume.id }) { volume ->
                    VolumeCard(volume = volume, onOpenCategory = onOpenCategory)
                }
            }
    }
}

/** Entry tile that drills into the largest-files view (FR-8.2). */
@Composable
private fun LargestFilesCard(onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Assessment,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(
                    text = stringResource(R.string.storage_largest_files_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.storage_largest_files_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun VolumeCard(
    volume: VolumeBreakdown,
    onOpenCategory: (StorageVolume, MediaCategory) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (volume.volume.isRemovable) Icons.Outlined.SdStorage else Icons.Outlined.Storage,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = volume.volume.label,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }

            val total = volume.volume.totalBytes
            val fraction = if (total > 0L) (volume.volume.usedBytes.toFloat() / total).coerceIn(0f, 1f) else 0f
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )
            Text(
                text =
                    stringResource(
                        R.string.storage_usage_summary,
                        Formatters.formatSize(volume.volume.usedBytes),
                        Formatters.formatSize(volume.volume.availableBytes),
                        Formatters.formatSize(total),
                    ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )

            volume.categories.forEach { usage ->
                CategoryRow(
                    usage = usage,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable { onOpenCategory(volume.volume, usage.category) }
                            .padding(vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun CategoryRow(
    usage: CategoryUsage,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = usage.category.icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(
                text = stringResource(usage.category.labelRes),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = pluralStringResource(R.plurals.storage_item_count, usage.itemCount, usage.itemCount),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = Formatters.formatSize(usage.sizeBytes),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/** Display label for each category on the breakdown rows. */
@get:StringRes
private val MediaCategory.labelRes: Int
    get() =
        when (this) {
            MediaCategory.Images -> R.string.storage_cat_images
            MediaCategory.Video -> R.string.storage_cat_video
            MediaCategory.Audio -> R.string.storage_cat_audio
            MediaCategory.Documents -> R.string.storage_cat_documents
            MediaCategory.Archives -> R.string.storage_cat_archives
            MediaCategory.Apps -> R.string.storage_cat_apps
            MediaCategory.Downloads -> R.string.storage_cat_downloads
            MediaCategory.Other -> R.string.storage_cat_other
        }

/** Outlined icon for each category. Kept in the Compose layer so the model stays pure. */
private val MediaCategory.icon: ImageVector
    get() =
        when (this) {
            MediaCategory.Images -> Icons.Outlined.Image
            MediaCategory.Video -> Icons.Outlined.Videocam
            MediaCategory.Audio -> Icons.Outlined.MusicNote
            MediaCategory.Documents -> Icons.Outlined.Description
            MediaCategory.Archives -> Icons.Outlined.FolderZip
            MediaCategory.Apps -> Icons.Outlined.Android
            MediaCategory.Downloads -> Icons.Outlined.Download
            MediaCategory.Other -> Icons.Outlined.InsertDriveFile
        }
