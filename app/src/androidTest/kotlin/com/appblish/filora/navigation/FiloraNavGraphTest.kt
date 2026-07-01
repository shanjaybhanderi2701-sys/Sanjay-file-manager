package com.appblish.filora.navigation

import android.Manifest
import android.os.Build
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.appblish.filora.HiltComponentActivity
import com.appblish.filora.core.ui.theme.FiloraTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * T166 (M16) — end-to-end navigation-graph traversal.
 *
 * Hosts the real [FiloraNavHost] (all feature screens, real Hilt view models) with a
 * [TestNavHostController] and walks the primary flow — Home → Browser → Search →
 * Media hub → Storage → Settings — asserting each destination is reached, then that
 * the back stack pops back through the pushed destinations to Home.
 *
 * Media read permission is granted so each screen's view model initialises cleanly.
 * Runs on the emulator matrix (`connectedAndroidTest`), not as a JVM unit test.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class FiloraNavGraphTest {
    private val hiltRule = HiltAndroidRule(this)
    private val grantRule = GrantPermissionRule.grant(*mediaReadPermissions())
    private val composeRule = createAndroidComposeRule<HiltComponentActivity>()

    @get:Rule
    val chain: RuleChain =
        RuleChain.outerRule(grantRule).around(hiltRule).around(composeRule)

    private lateinit var navController: TestNavHostController

    @Before
    fun setUp() {
        composeRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())
            FiloraTheme {
                FiloraNavHost(
                    startDestination = Route.Home,
                    navController = navController,
                )
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun startsAtHome() {
        assertTrue(navController.currentDestination?.hasRoute<Route.Home>() == true)
    }

    @Test
    fun traversesPrimaryDestinations() {
        navigateTo(Route.Browser(location = ""))
        assertTrue(navController.currentDestination?.hasRoute<Route.Browser>() == true)

        navigateTo(Route.Search())
        assertTrue(navController.currentDestination?.hasRoute<Route.Search>() == true)

        navigateTo(Route.MediaHub)
        assertTrue(navController.currentDestination?.hasRoute<Route.MediaHub>() == true)

        navigateTo(Route.Storage)
        assertTrue(navController.currentDestination?.hasRoute<Route.Storage>() == true)

        navigateTo(Route.Settings)
        assertTrue(navController.currentDestination?.hasRoute<Route.Settings>() == true)
    }

    @Test
    fun backStackPopsToPreviousDestinations() {
        navigateTo(Route.Browser(location = ""))
        navigateTo(Route.Settings)

        popBackStack()
        assertTrue(navController.currentDestination?.hasRoute<Route.Browser>() == true)

        popBackStack()
        assertTrue(navController.currentDestination?.hasRoute<Route.Home>() == true)
    }

    private fun navigateTo(route: Route) {
        composeRule.runOnUiThread { navController.navigate(route) }
        composeRule.waitForIdle()
    }

    private fun popBackStack() {
        composeRule.runOnUiThread { navController.popBackStack() }
        composeRule.waitForIdle()
    }
}

/** Read permissions for the current API level (mirrors [HomeSmokeTest]). */
private fun mediaReadPermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO,
        )
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
