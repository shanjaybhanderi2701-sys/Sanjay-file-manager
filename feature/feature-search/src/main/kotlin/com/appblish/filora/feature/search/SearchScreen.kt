package com.appblish.filora.feature.search

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.appblish.filora.core.ui.component.EmptyState

/**
 * M1 placeholder for the Search screen. Behaviour and ViewModel are implemented in
 * a later milestone (see docs/phase-1/11-engineering-backlog.md); this stub keeps
 * the module and navigation graph buildable.
 */
@Composable
fun SearchScreen(modifier: Modifier = Modifier) {
    EmptyState(
        icon = Icons.Outlined.Search,
        title = "Search",
        description = "Coming soon.",
        modifier = modifier,
    )
}
