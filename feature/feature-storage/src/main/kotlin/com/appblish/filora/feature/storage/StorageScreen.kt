package com.appblish.filora.feature.storage

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.appblish.filora.core.ui.component.EmptyState

/**
 * M1 placeholder for the Storage screen. Behaviour and ViewModel are implemented in
 * a later milestone (see docs/phase-1/11-engineering-backlog.md); this stub keeps
 * the module and navigation graph buildable.
 */
@Composable
fun StorageScreen(modifier: Modifier = Modifier) {
    EmptyState(
        icon = Icons.Outlined.Storage,
        title = "Storage",
        description = "Coming soon.",
        modifier = modifier,
    )
}
