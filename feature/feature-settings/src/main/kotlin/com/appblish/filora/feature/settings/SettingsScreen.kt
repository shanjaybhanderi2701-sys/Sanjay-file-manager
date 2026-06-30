package com.appblish.filora.feature.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.appblish.filora.core.ui.component.EmptyState

/**
 * M1 placeholder for the Settings screen. Behaviour and ViewModel are implemented in
 * a later milestone (see docs/phase-1/11-engineering-backlog.md); this stub keeps
 * the module and navigation graph buildable.
 */
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    EmptyState(
        icon = Icons.Outlined.Settings,
        title = "Settings",
        description = "Coming soon.",
        modifier = modifier,
    )
}
