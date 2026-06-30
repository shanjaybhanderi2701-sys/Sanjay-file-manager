package com.appblish.filora.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.appblish.filora.feature.browser.BrowserScreen
import com.appblish.filora.feature.home.HomeScreen
import com.appblish.filora.feature.media.MediaCategoryScreen
import com.appblish.filora.feature.search.SearchScreen
import com.appblish.filora.feature.settings.SettingsScreen
import com.appblish.filora.feature.storage.StorageScreen

/**
 * Single-activity navigation graph. Start destination is [Route.Home]; feature
 * destinations use type-safe routes. Screens beyond Home are M1 placeholders.
 */
@Composable
fun FiloraNavHost(navController: NavHostController = rememberNavController(),) {
    NavHost(
        navController = navController,
        startDestination = Route.Home,
    ) {
        composable<Route.Home> {
            HomeScreen(
                onOpenSettings = { navController.navigate(Route.Settings) },
            )
        }
        composable<Route.Browser> { BrowserScreen() }
        composable<Route.Search> { SearchScreen() }
        composable<Route.Media> { MediaCategoryScreen() }
        composable<Route.Storage> { StorageScreen() }
        composable<Route.Settings> { SettingsScreen() }
    }
}
