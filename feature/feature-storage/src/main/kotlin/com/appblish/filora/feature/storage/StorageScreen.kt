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
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.appblish.filora.core.ui.theme.FiloraElevation
import com.appblish.filora.core.ui.theme.FiloraMotion
import com.appblish.filora.core.ui.theme.FiloraSpacing

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
                contentPadding = PaddingValues(FiloraSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(FiloraSpacing.lg),
            ) {
                val story = StorageStory.from(uiState.breakdown)
                if (story.hasData) {
                    item(key = "storage-story") {
                        StorageStoryHero(story = story)
                    }
                }
                item(key = "largest-files") {
                    LargestFilesCard(onClick = onOpenLargestFiles)
                }
                items(volumes, key = { it.volume.id }) { volume ->
                    VolumeCard(volume = volume, onOpenCategory = onOpenCategory)
                }
            }
    }
}

/**
 * Storage-story hero band (hi-fi spec §3.5 — the Direction-C borrowing).
 *
 * A *calm* year-in-review on the Storage surface only: the big free-space number
 * (`displaySmall`) counts up once, a segmented by-category bar grows once, and a single
 * honest line names where space is going. It is `tertiaryContainer` so it reads as
 * "insight", distinct from the teal primary action color (spec §1.1). Everything is
 * derived from the already-computed [StorageStory] — no second scan (principle #3) — and
 * the one-shot reveal honors the system animation scale / reduce-motion.
 */
@Composable
private fun StorageStoryHero(
    story: StorageStory,
    modifier: Modifier = Modifier,
) {
    val motionEnabled = rememberStoryMotionEnabled()
    var hasRevealed by rememberSaveable { mutableStateOf(false) }
    val reveal = remember { Animatable(if (motionEnabled && !hasRevealed) 0f else 1f) }
    LaunchedEffect(story.totalBytes) {
        if (motionEnabled && !hasRevealed && story.hasData) {
            reveal.snapTo(0f)
            reveal.animateTo(
                targetValue = 1f,
                animationSpec = tween(FiloraMotion.storyReveal, easing = FiloraMotion.EmphasizedDecelerate),
            )
        } else {
            reveal.snapTo(1f)
        }
        hasRevealed = true
    }
    val progress = reveal.value
    val shownFree = (story.freeBytes * progress).toLong()

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        tonalElevation = FiloraElevation.level1,
    ) {
        Column(modifier = Modifier.padding(FiloraSpacing.xl)) {
            Text(
                text = Formatters.formatSize(shownFree),
                style = MaterialTheme.typography.displaySmall,
            )
            Text(
                text = stringResource(R.string.storage_story_free_caption, Formatters.formatSize(story.totalBytes)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
            )
            StorageStoryBar(
                story = story,
                progress = progress,
                modifier = Modifier.fillMaxWidth().padding(top = FiloraSpacing.lg),
            )
            Text(
                text = storyHeadline(story),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = FiloraSpacing.md),
            )
        }
    }
}

/** The one motivating line: name the top category, or a neutral used/total summary. */
@Composable
private fun storyHeadline(story: StorageStory): String {
    val top = story.topCategory
    return if (top != null) {
        stringResource(R.string.storage_story_top_category, stringResource(top.labelRes))
    } else {
        stringResource(
            R.string.storage_story_used_summary,
            Formatters.formatSize(story.usedBytes),
            Formatters.formatSize(story.totalBytes),
        )
    }
}

/**
 * Calm segmented bar: a single rounded track whose filled width is the used fraction,
 * split into tonal slices (descending `onTertiaryContainer` alpha — no type-coded
 * colors, spec §1.1 rule 1). The fill grows once via `scaleX` anchored on the leading
 * edge; RTL is handled by Compose laying the track out mirrored.
 */
@Composable
private fun StorageStoryBar(
    story: StorageStory,
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.onTertiaryContainer
    val barDesc =
        stringResource(
            R.string.storage_story_bar_a11y,
            Formatters.formatSize(story.usedBytes),
            Formatters.formatSize(story.totalBytes),
        )
    Box(
        modifier =
            modifier
                .height(FiloraSpacing.md)
                .clip(MaterialTheme.shapes.small)
                .background(accent.copy(alpha = 0.12f))
                .semantics { contentDescription = barDesc },
    ) {
        if (story.usedFraction > 0f) {
            StorageStoryBarFill(story = story, accent = accent, progress = progress)
        }
    }
}

/** The filled (used) portion of the bar — tonal category segments that grow once. */
@Composable
private fun StorageStoryBarFill(
    story: StorageStory,
    accent: Color,
    progress: Float,
) {
    val used = story.usedBytes.toFloat().coerceAtLeast(1f)
    Row(
        modifier =
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(story.usedFraction)
                .graphicsLayer {
                    scaleX = progress
                    transformOrigin = TransformOrigin(0f, 0.5f)
                },
    ) {
        story.slices.forEachIndexed { index, slice ->
            val weight = slice.sizeBytes / used
            val alpha = (0.9f - index * 0.12f).coerceIn(0.35f, 0.9f)
            if (weight > 0f) StorageStorySegment(weight = weight, color = accent.copy(alpha = alpha))
        }
        val uncategorizedWeight = story.uncategorizedBytes / used
        if (uncategorizedWeight > 0f) {
            StorageStorySegment(weight = uncategorizedWeight, color = accent.copy(alpha = 0.25f))
        }
    }
}

/** One weighted, full-height segment of the storage-story bar. */
@Composable
private fun RowScope.StorageStorySegment(
    weight: Float,
    color: Color,
) {
    Box(
        modifier =
            Modifier
                .fillMaxHeight()
                .weight(weight)
                .background(color),
    )
}

/** True unless the system animation scale is 0 (reduce-motion / "Remove animations"). */
@Composable
private fun rememberStoryMotionEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) != 0f
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
