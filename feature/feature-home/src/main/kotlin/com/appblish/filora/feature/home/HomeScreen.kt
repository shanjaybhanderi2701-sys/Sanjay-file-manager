package com.appblish.filora.feature.home

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appblish.filora.core.ui.component.EmptyState

/**
 * Home dashboard. M1 renders an empty themed state proving the app installs and
 * launches end-to-end; data-backed sections arrive in later milestones.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Filora") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
    ) { padding ->
        if (uiState.isEmpty || uiState.isLoading) {
            EmptyState(
                icon = Icons.Outlined.FolderOpen,
                title = "Welcome to Filora",
                description = "Your files will appear here. Grant storage access to get started.",
                modifier = Modifier.padding(padding),
            )
        }
    }
}
