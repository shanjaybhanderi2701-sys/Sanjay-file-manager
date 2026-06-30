package com.appblish.filora.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes (Navigation-Compose 2.8+). Params are primitives so
 * no cross-module serialization config is needed; the data layer interprets
 * [Browser.location] (a path or tree-uri string) and [Media.category].
 */
sealed interface Route {
    @Serializable data object Home : Route

    @Serializable data class Browser(
        val location: String
    ) : Route

    @Serializable data class Search(
        val scope: String? = null
    ) : Route

    @Serializable data class Media(
        val category: String
    ) : Route

    @Serializable data object Storage : Route

    @Serializable data class LargestFiles(
        val volumeId: String? = null
    ) : Route

    @Serializable data object Settings : Route

    @Serializable data object About : Route
}
