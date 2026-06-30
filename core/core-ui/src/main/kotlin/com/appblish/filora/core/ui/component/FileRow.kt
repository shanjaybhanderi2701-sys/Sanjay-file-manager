package com.appblish.filora.core.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Single row in a directory listing. Selection/overflow actions are layered on by
 * the feature; this renders name + subtitle + leading icon, and tints the container
 * when [selected] so multi-select state is visible (FR-4.1).
 */
@Composable
fun FileRow(
    name: String,
    subtitle: String,
    isDirectory: Boolean,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
) {
    val containerColor =
        if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Unspecified
    ListItem(
        modifier = modifier,
        colors = ListItemDefaults.colors(containerColor = containerColor),
        headlineContent = {
            Text(text = name, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Column(modifier = Modifier.padding(top = 2.dp)) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        leadingContent = {
            Icon(
                imageVector = if (isDirectory) Icons.Outlined.Folder else Icons.Outlined.InsertDriveFile,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
    )
}
