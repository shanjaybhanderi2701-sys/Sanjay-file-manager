package com.appblish.filora.core.data.search

import com.appblish.filora.core.common.util.FileExtensions
import com.appblish.filora.core.domain.model.FileItem
import com.appblish.filora.core.domain.model.SearchQuery
import java.io.File

/** Tuning shared by the java.io and SAF tree walks. */
internal object SearchTraversal {
    /**
     * Hard recursion bound. The java.io walk also dedups visited canonical paths to break
     * symlink loops, but the depth cap is a cheap second guard (and the only one for SAF,
     * which has no symlinks) so a pathologically deep tree can never spin forever.
     */
    const val MAX_DEPTH = 64

    /** Unix dotfile convention; entries are skipped unless [SearchQuery.includeHidden]. */
    const val HIDDEN_PREFIX = "."
}

/**
 * True when [item] passes the query: its name contains the (already-trimmed) [needle]
 * case-insensitively — or the needle is blank, meaning "match every name" so a pure
 * size/date/type filter still streams — AND it satisfies the AND-combined
 * [SearchQuery.filter]. The filter excludes directories whenever any dimension is active.
 */
internal fun SearchQuery.accepts(
    item: FileItem,
    needle: String,
): Boolean = (needle.isBlank() || item.name.contains(needle, ignoreCase = true)) && filter.matches(item)

/** Maps a java.io entry to the platform-neutral [FileItem]; size is 0 for directories. */
internal fun File.toFileItem(isDirectory: Boolean): FileItem =
    FileItem(
        name = name,
        path = absolutePath,
        isDirectory = isDirectory,
        sizeBytes = if (isDirectory) 0L else length(),
        lastModifiedEpochMillis = lastModified(),
        extension = FileExtensions.extensionOf(name),
        isHidden = name.startsWith(SearchTraversal.HIDDEN_PREFIX),
    )
