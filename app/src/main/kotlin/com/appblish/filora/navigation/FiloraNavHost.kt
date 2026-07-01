package com.appblish.filora.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.appblish.filora.BuildConfig
import com.appblish.filora.core.domain.model.MediaCategory
import com.appblish.filora.feature.browser.BrowserScreen
import com.appblish.filora.feature.home.HomeScreen
import com.appblish.filora.feature.media.MediaCategoryDetailScreen
import com.appblish.filora.feature.media.MediaCategoryScreen
import com.appblish.filora.feature.search.SearchScreen
import com.appblish.filora.feature.settings.AboutScreen
import com.appblish.filora.feature.settings.SettingsScreen
import com.appblish.filora.feature.storage.LargestFilesScreen
import com.appblish.filora.feature.storage.RecycleBinScreen
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
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
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
                onOpenCategory = { category -> navController.navigate(Route.Media(category.name)) },
                onOpenStorage = { navController.navigate(Route.Storage) },
                onBrowse = { navController.navigate(Route.Browser(location = "")) },
                onOpenItem = { item -> navController.navigate(homeItemRoute(item)) },
            )
        }
        composable<Route.Browser>(
            deepLinks = listOf(navDeepLink<Route.Browser>(basePath = FiloraDeepLinks.BROWSER)),
        ) { backStackEntry ->
            BrowserScreen(
                location = backStackEntry.toRoute<Route.Browser>().location,
                onOpenDirectory = { path -> navController.navigate(Route.Browser(location = path)) },
                onNavigateToAncestor = { path -> navController.navigateToAncestor(path) },
            )
        }
        composable<Route.Search> { backStackEntry ->
            SearchScreen(
                scope = backStackEntry.toRoute<Route.Search>().scope,
                onOpenResult = { item -> navController.navigate(homeItemRoute(item)) },
            )
        }
        composable<Route.MediaHub>(
            deepLinks = listOf(navDeepLink<Route.MediaHub>(basePath = FiloraDeepLinks.CATEGORIES)),
        ) {
            MediaCategoryScreen(
                onOpenCategory = { category -> navController.navigate(Route.Media(category.name)) },
            )
        }
        composable<Route.Media>(
            deepLinks = listOf(navDeepLink<Route.Media>(basePath = FiloraDeepLinks.CATEGORY)),
        ) { backStackEntry ->
            val category = MediaCategory.valueOf(backStackEntry.toRoute<Route.Media>().category)
            MediaCategoryDetailScreen(category = category)
        }
        composable<Route.Storage> {
            StorageScreen(
                // Volume-filtered hub lands when the media detail accepts a volume scope;
                // for now a category tap opens that category's hub (FR-8.1 interaction).
                onOpenCategory = { _, category -> navController.navigate(Route.Media(category.name)) },
                onOpenLargestFiles = { navController.navigate(Route.LargestFiles()) },
                onOpenRecycleBin = { navController.navigate(Route.RecycleBin) },
            )
        }
        composable<Route.LargestFiles> { backStackEntry ->
            LargestFilesScreen(volumeId = backStackEntry.toRoute<Route.LargestFiles>().volumeId)
        }
        composable<Route.RecycleBin> {
            RecycleBinScreen(onNavigateUp = { navController.navigateUp() })
        }
        composable<Route.Settings> {
            SettingsScreen(onOpenAbout = { navController.navigate(Route.About) })
        }
        composable<Route.About> { AboutScreen(versionName = BuildConfig.VERSION_NAME) }
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

/**
 * Breadcrumb ancestor tap (T048/T049): walk *up* to [location] rather than pushing a new
 * level. If that ancestor is already on the back stack (the normal descend-then-tap case)
 * its entries-above are popped and it is reused; if it isn't (e.g. a deep link landed us
 * deep in the tree) we simply navigate to it. Either way the trail never accumulates
 * duplicate folder entries.
 */
private fun NavHostController.navigateToAncestor(location: String) {
    navigate(Route.Browser(location = location)) {
        popUpTo(Route.Browser(location = location)) { inclusive = false }
        launchSingleTop = true
    }
}

/**
 * Deep-link base paths (T053). The single `filora://` scheme is registered on
 * [MainActivity][com.appblish.filora.MainActivity] in the manifest; the typed
 * [navDeepLink] builders below expand each base into the route's argument template
 * (e.g. `filora://browser?location={location}`), so an external intent can open a
 * specific folder or category hub straight into the graph.
 */
internal object FiloraDeepLinks {
    const val SCHEME = "filora"
    const val BROWSER = "$SCHEME://browser"
    const val CATEGORY = "$SCHEME://category"
    const val CATEGORIES = "$SCHEME://categories"
}
