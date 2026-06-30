package com.appblish.filora.navigation

import com.appblish.filora.core.domain.model.FileItem

/**
 * Maps a Home recents/favorites tap (APP-101 / T6.5a) onto a navigation [Route].
 *
 * A directory opens directly in the file browser at its own location; a file opens
 * the browser at its containing folder so the user lands next to the item (the file
 * itself is then a tap away, and open/share live in the browser's per-item actions).
 * [FileItem.path] is an opaque locator (a filesystem path or a tree-document URI
 * string) — we only split it on the last `/` segment, which both forms share, and
 * never branch on its scheme.
 *
 * Pulled out as a pure function so the routing decision is unit-testable without a
 * Compose/Navigation runtime, the same way [com.appblish.filora.permission.StoragePermissions]
 * keeps its mapping host-testable.
 */
internal fun homeItemRoute(item: FileItem): Route =
    if (item.isDirectory) {
        Route.Browser(location = item.path)
    } else {
        Route.Browser(location = parentLocation(item.path))
    }

/**
 * The containing folder of [path]: everything before the final `/` segment. A path
 * with no separator (or only a leading-root one) resolves to itself, so a tap is
 * always a valid Browser location rather than an empty string.
 */
private fun parentLocation(path: String): String {
    val lastSeparator = path.lastIndexOf('/')
    return if (lastSeparator > 0) path.substring(0, lastSeparator) else path
}
