package com.appblish.filora.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes (Navigation-Compose 2.8+). Params are primitives so
 * no cross-module serialization config is needed; the data layer interprets
 * [Browser.location] (a path or tree-uri string) and [Media.category].
 */
sealed interface Route {
    /** First-run storage permission rationale + request flow (FR-1.1). */
    @Serializable data object Permission : Route

    @Serializable data object Home : Route

    @Serializable data class Browser(
        val location: String
    ) : Route

    @Serializable data class Search(
        val scope: String? = null
    ) : Route

    /** The category-hub grid (FR-6.1); [Media] is a single category's detail list. */
    @Serializable data object MediaHub : Route

    @Serializable data class Media(
        val category: String
    ) : Route

    @Serializable data object Storage : Route

    @Serializable data class LargestFiles(
        val volumeId: String? = null
    ) : Route

    /** App-managed recycle bin: restore or permanently remove deleted files (FR-3.4). */
    @Serializable data object RecycleBin : Route

    @Serializable data object Settings : Route

    @Serializable data object About : Route
}
