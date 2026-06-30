package com.appblish.filora.feature.browser

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.appblish.filora.core.ui.component.EmptyState

/**
 * M1 placeholder for the Browser screen. Behaviour and ViewModel are implemented in
 * a later milestone (see docs/phase-1/11-engineering-backlog.md); this stub keeps
 * the module and navigation graph buildable.
 */
@Composable
fun BrowserScreen(modifier: Modifier = Modifier) {
    EmptyState(
        icon = Icons.Outlined.Folder,
        title = "Browser",
        description = "Coming soon.",
        modifier = modifier,
    )
}
