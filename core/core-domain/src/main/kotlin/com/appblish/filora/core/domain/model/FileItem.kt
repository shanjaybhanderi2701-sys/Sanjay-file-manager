package com.appblish.filora.core.domain.model

/**
 * A file or directory entry, decoupled from any platform source (java.io / SAF /
 * MediaStore). [path] is an opaque locator — a filesystem path or a tree-document
 * URI string — interpreted by the data layer, never branched on by use cases.
 */
data class FileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val lastModifiedEpochMillis: Long,
    val mimeType: String? = null,
    val extension: String = "",
    val isHidden: Boolean = false,
    val childCount: Int? = null,
)
