package com.appblish.filora.feature.browser

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.VideoFile
import androidx.compose.ui.graphics.vector.ImageVector
import com.appblish.filora.core.common.util.FileCategory
import com.appblish.filora.core.common.util.FileExtensions
import com.appblish.filora.core.domain.model.FileItem

/**
 * Type-icon mapping for a directory entry (T039). Folders get the folder glyph; files map
 * by extension-derived [FileCategory] so the grid and list read at a glance without
 * touching I/O or MIME.
 */
internal fun fileIcon(item: FileItem): ImageVector =
    if (item.isDirectory) {
        Icons.Outlined.Folder
    } else {
        when (FileExtensions.categoryOf(item.name)) {
            FileCategory.Image -> Icons.Outlined.Image
            FileCategory.Video -> Icons.Outlined.VideoFile
            FileCategory.Audio -> Icons.Outlined.AudioFile
            FileCategory.Document -> Icons.Outlined.Article
            FileCategory.Archive -> Icons.Outlined.FolderZip
            FileCategory.Apk -> Icons.Outlined.Android
            FileCategory.Other -> Icons.Outlined.InsertDriveFile
        }
    }
