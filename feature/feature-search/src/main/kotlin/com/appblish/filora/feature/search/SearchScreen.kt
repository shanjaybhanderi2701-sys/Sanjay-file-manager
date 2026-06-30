package com.appblish.filora.feature.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appblish.filora.core.domain.model.FileItem
import com.appblish.filora.core.domain.model.FileTypeFilter
import com.appblish.filora.core.ui.component.EmptyState
import com.appblish.filora.core.ui.component.FileRow

/**
 * Search screen (FR-5.1 streaming name search, FR-5.2 type/size/date filters). The
 * query field drives a cancelable, debounced tree walk; type/size/date chips narrow
 * the streamed results and the active selections show as removable chips above the
 * list. [scope] is the directory the search traverses (a path or tree-uri).
 */
@Composable
fun SearchScreen(
    scope: String? = null,
    onOpenResult: (FileItem) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(scope) { viewModel.bindScope(scope) }

    SearchContent(
        uiState = uiState,
        onQueryChange = viewModel::onQueryChange,
        onToggleType = viewModel::toggleType,
        onSelectSize = viewModel::selectSize,
        onSelectDate = viewModel::selectDate,
        onRemoveChip = viewModel::removeChip,
        onOpenResult = onOpenResult,
        modifier = modifier,
    )
}

@Composable
internal fun SearchContent(
    uiState: SearchUiState,
    onQueryChange: (String) -> Unit,
    onToggleType: (FileTypeFilter) -> Unit,
    onSelectSize: (SizeBucket?) -> Unit,
    onSelectDate: (DateBucket?) -> Unit,
    onRemoveChip: (ActiveFilterChip) -> Unit,
    onOpenResult: (FileItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = uiState.query,
            onValueChange = onQueryChange,
            singleLine = true,
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            placeholder = { Text("Search files") },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
        )

        FilterChipRows(
            uiState = uiState,
            onToggleType = onToggleType,
            onSelectSize = onSelectSize,
            onSelectDate = onSelectDate,
        )

        if (uiState.activeChips.isNotEmpty()) {
            ActiveChipRow(chips = uiState.activeChips, onRemoveChip = onRemoveChip)
        }

        if (uiState.isSearching) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        SearchResults(uiState = uiState, onOpenResult = onOpenResult)
    }
}

@Composable
private fun FilterChipRows(
    uiState: SearchUiState,
    onToggleType: (FileTypeFilter) -> Unit,
    onSelectSize: (SizeBucket?) -> Unit,
    onSelectDate: (DateBucket?) -> Unit,
) {
    ChipScrollRow {
        FileTypeFilter.entries.forEach { type ->
            FilterChip(
                selected = type in uiState.filter.types,
                onClick = { onToggleType(type) },
                label = { Text(type.chipLabel) },
            )
        }
    }
    ChipScrollRow {
        SizeBucket.entries.forEach { bucket ->
            FilterChip(
                selected = bucket == uiState.selectedSize,
                onClick = { onSelectSize(bucket) },
                label = { Text(bucket.label) },
            )
        }
    }
    ChipScrollRow {
        DateBucket.entries.forEach { bucket ->
            FilterChip(
                selected = bucket == uiState.selectedDate,
                onClick = { onSelectDate(bucket) },
                label = { Text(bucket.label) },
            )
        }
    }
}

@Composable
private fun ActiveChipRow(
    chips: List<ActiveFilterChip>,
    onRemoveChip: (ActiveFilterChip) -> Unit,
) {
    ChipScrollRow {
        chips.forEach { chip ->
            InputChip(
                selected = true,
                onClick = { onRemoveChip(chip) },
                label = { Text(chip.label) },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Remove ${chip.label} filter",
                    )
                },
            )
        }
    }
}

@Composable
private fun SearchResults(
    uiState: SearchUiState,
    onOpenResult: (FileItem) -> Unit,
) {
    if (uiState.isEmpty) {
        EmptyState(
            icon = Icons.Outlined.Search,
            title = "No matches",
            description = "Try a different name or clear some filters.",
        )
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(uiState.results, key = { it.path }) { item ->
                FileRow(
                    name = item.name,
                    subtitle = item.subtitle,
                    isDirectory = item.isDirectory,
                    modifier = Modifier.clickable { onOpenResult(item) },
                )
            }
        }
    }
}

@Composable
private fun ChipScrollRow(content: @Composable () -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
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
