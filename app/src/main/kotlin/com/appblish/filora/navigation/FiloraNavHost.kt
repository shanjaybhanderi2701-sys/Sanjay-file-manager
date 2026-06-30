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
import com.appblish.filora.permission.PermissionRationaleScreen

/**
 * Single-activity navigation graph. [startDestination] is [Route.Permission] on a
 * first run that lacks storage access and [Route.Home] otherwise (see
 * [MainActivity][com.appblish.filora.MainActivity]). Feature destinations use
 * type-safe routes; screens beyond Home are M1 placeholders.
 */
@Composable
fun FiloraNavHost(
    startDestination: Route = Route.Home,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable<Route.Permission> {
            PermissionRationaleScreen(
                onGranted = { navController.navigateToHomeFromGate() },
                onContinueWithLimitedAccess = { navController.navigateToHomeFromGate() },
            )
        }
        composable<Route.Home> {
            HomeScreen(
                onOpenSettings = { navController.navigate(Route.Settings) },
            )
        }
        composable<Route.Browser> { BrowserScreen() }
        composable<Route.Search> { SearchScreen() }
        composable<Route.Media> {
            // Category file lists land in a later M4 task; the hub renders counts now.
            MediaCategoryScreen(onOpenCategory = {})
        }
        composable<Route.Storage> { StorageScreen() }
        composable<Route.Settings> { SettingsScreen() }
    }
}

/**
 * Leave the permission gate for Home, clearing it from the back stack so system
 * Back never returns to the rationale (nav-flow doc §3: `popUpTo(home)`).
 */
private fun NavHostController.navigateToHomeFromGate() {
    navigate(Route.Home) {
        popUpTo(Route.Permission) { inclusive = true }
        launchSingleTop = true
    }
}
