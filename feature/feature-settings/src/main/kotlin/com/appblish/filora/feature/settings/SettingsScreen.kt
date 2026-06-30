package com.appblish.filora.feature.settings

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appblish.filora.core.domain.model.SortOrder
import com.appblish.filora.core.domain.model.ThemeMode
import com.appblish.filora.core.domain.model.UserPreferences
import com.appblish.filora.core.domain.model.ViewLayout

/**
 * Settings screen (M7 T7.1, FR-11.1/FR-11.2). Stateful entry wired into the nav
 * graph: it observes the DataStore-backed [SettingsViewModel] and forwards every
 * change straight to persistence, so the rendered state is always the stored one.
 */
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsContent(
        preferences = uiState.preferences,
        modifier = modifier,
        actions =
            SettingsActions(
                onThemeMode = viewModel::setThemeMode,
                onDynamicColor = viewModel::setUseDynamicColor,
                onShowHidden = viewModel::setShowHiddenFiles,
                onViewLayout = viewModel::setDefaultViewLayout,
                onSortBy = { by ->
                    viewModel.setDefaultSortOrder(uiState.preferences.defaultSortOrder.copy(by = by))
                },
                onSortAscending = { asc ->
                    viewModel.setDefaultSortOrder(uiState.preferences.defaultSortOrder.copy(ascending = asc))
                },
                onFoldersFirst = { first ->
                    viewModel.setDefaultSortOrder(uiState.preferences.defaultSortOrder.copy(foldersFirst = first))
                },
            ),
    )
}

/**
 * Stateless rendering of the settings surface. Hoisted out of [SettingsScreen] so
 * the layout can be exercised in previews and unit tests without Hilt or DataStore.
 */
@Composable
fun SettingsContent(
    preferences: UserPreferences,
    modifier: Modifier = Modifier,
    actions: SettingsActions = SettingsActions(),
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // FR-11.1 — Appearance.
        SectionHeader("Appearance")
        SettingRow(label = "Theme") {
            ChipGroup(
                options = ThemeMode.entries,
                selected = preferences.themeMode,
                label = { it.label() },
                onSelect = actions.onThemeMode,
                testTagPrefix = "theme",
            )
        }
        SwitchRow(
            label = "Dynamic color",
            description =
                if (supportsDynamicColor) {
                    "Use colors from your wallpaper (Material You)."
                } else {
                    "Requires Android 12 or newer."
                },
            checked = preferences.useDynamicColor && supportsDynamicColor,
            onCheckedChange = actions.onDynamicColor,
            testTag = "dynamic_color",
            enabled = supportsDynamicColor,
        )

        HorizontalDivider()

        // FR-11.2 — Browsing defaults.
        SectionHeader("Files")
        SwitchRow(
            label = "Show hidden files",
            description = "Display files and folders that start with a dot.",
            checked = preferences.showHiddenFiles,
            onCheckedChange = actions.onShowHidden,
            testTag = "show_hidden",
        )
        SettingRow(label = "Default view") {
            ChipGroup(
                options = ViewLayout.entries,
                selected = preferences.defaultViewLayout,
                label = { it.name },
                onSelect = actions.onViewLayout,
                testTagPrefix = "view",
            )
        }
        SettingRow(label = "Sort by") {
            ChipGroup(
                options = SortOrder.By.entries,
                selected = preferences.defaultSortOrder.by,
                label = { it.label() },
                onSelect = actions.onSortBy,
                testTagPrefix = "sort",
            )
        }
        SwitchRow(
            label = "Ascending order",
            description = "Smallest / A→Z / oldest first.",
            checked = preferences.defaultSortOrder.ascending,
            onCheckedChange = actions.onSortAscending,
            testTag = "sort_ascending",
        )
        SwitchRow(
            label = "Folders first",
            description = "Group folders before files.",
            checked = preferences.defaultSortOrder.foldersFirst,
            onCheckedChange = actions.onFoldersFirst,
            testTag = "folders_first",
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun SettingRow(
    label: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        content()
    }
}

@Composable
private fun SwitchRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            modifier = Modifier.testTag(testTag),
        )
    }
}

@Composable
private fun <T> ChipGroup(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    testTagPrefix: String,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(label(option)) },
                modifier = Modifier.testTag("${testTagPrefix}_${label(option)}"),
            )
        }
    }
}

private val supportsDynamicColor: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

private fun ThemeMode.label(): String =
    when (this) {
        ThemeMode.System -> "System"
        ThemeMode.Light -> "Light"
        ThemeMode.Dark -> "Dark"
    }

private fun SortOrder.By.label(): String =
    when (this) {
        SortOrder.By.Name -> "Name"
        SortOrder.By.Size -> "Size"
        SortOrder.By.DateModified -> "Date"
        SortOrder.By.Type -> "Type"
    }
