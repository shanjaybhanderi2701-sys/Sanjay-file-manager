package com.appblish.filora

import android.Manifest
import android.os.Build
import androidx.activity.compose.setContent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.hasRoute
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.appblish.filora.core.domain.model.MediaCategory
import com.appblish.filora.navigation.FiloraNavHost
import com.appblish.filora.navigation.Route
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * T166 — end-to-end navigation contract over the real [FiloraNavHost] graph.
 *
 * Rather than depend on which destinations happen to have a UI entry point in the
 * current milestone (Search and the media-category hub have none yet, so a pure
 * click-through can't reach them), this drives a [TestNavHostController] through the
 * *actual* type-safe [Route] graph. Assertions are on the destination route type
 * ([hasRoute]) — locale- and label-independent, so they don't break when copy or
 * translations change.
 *
 * It doubles as the T165 reachability guard: every key screen must compose without
 * crashing when navigated to (a Hilt-injected ViewModel that fails to construct would
 * throw here). Screen-level content assertions live in [KeyScreensRenderTest].
 *
 * Runs on an emulator/device (`connectedStandardDebugAndroidTest`), not a JVM unit
 * test — the NavHost hosts `hiltViewModel()` screens, so it needs the Hilt-wired app.
 *
 * NOTE: authored where no JDK/emulator is available; verified by inspection against
 * the real symbols, CI-green is observed on the instrumented-matrix lane after merge.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class NavGraphNavigationTest {
    private val hiltRule = HiltAndroidRule(this)

    // Media access granted so screens whose ViewModels read media compose cleanly.
    private val grantRule = GrantPermissionRule.grant(*mediaReadPermissions())
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val chain: RuleChain =
        RuleChain.outerRule(grantRule).around(hiltRule).around(composeRule)

    private lateinit var navController: TestNavHostController

    @Before
    fun setUp() {
        // Replace the activity's own content with the graph under test, wired to a
        // TestNavHostController we can drive and inspect. MainActivity is
        // @AndroidEntryPoint, so `hiltViewModel()` on the nav back-stack entries
        // resolves against the real Hilt graph.
        composeRule.runOnUiThread {
            composeRule.activity.setContent {
                navController =
                    TestNavHostController(LocalContext.current).apply {
                        navigatorProvider.addNavigator(ComposeNavigator())
                    }
                FiloraNavHost(startDestination = Route.Home, navController = navController)
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun startsOnHome() {
        assertTrue(currentRouteIs<Route.Home>())
    }

    @Test
    fun homeToSettingsToAbout_backStackUnwinds() {
        navigateTo(Route.Settings)
        assertTrue(currentRouteIs<Route.Settings>())

        navigateTo(Route.About)
        assertTrue(currentRouteIs<Route.About>())

        // Back returns to Settings, then Home — the trail unwinds in order.
        popBack()
        assertTrue(currentRouteIs<Route.Settings>())
        popBack()
        assertTrue(currentRouteIs<Route.Home>())
    }

    @Test
    fun browserDescendKeepsEachLevelOnTheBackStack() {
        navigateTo(Route.Browser(location = ""))
        assertTrue(currentRouteIs<Route.Browser>())

        // Descending pushes a new Browser entry per level (T031/T048 back semantics).
        navigateTo(Route.Browser(location = "/sdcard/Download"))
        assertTrue(currentRouteIs<Route.Browser>())

        popBack()
        assertTrue(currentRouteIs<Route.Browser>())
        popBack()
        assertTrue(currentRouteIs<Route.Home>())
    }

    @Test
    fun storageToRecycleBin_andBack() {
        navigateTo(Route.Storage)
        assertTrue(currentRouteIs<Route.Storage>())

        navigateTo(Route.RecycleBin)
        assertTrue(currentRouteIs<Route.RecycleBin>())

        popBack()
        assertTrue(currentRouteIs<Route.Storage>())
    }

    @Test
    fun everyKeyScreenIsReachableAndComposes() {
        // The five key screens from the M16 charter, including the two with no UI
        // entry point yet (media hub, search). Each navigation composes the real
        // screen; a broken DI graph or crashing screen would fail here.
        navigateTo(Route.Home)
        assertTrue(currentRouteIs<Route.Home>())

        navigateTo(Route.Browser(location = ""))
        assertTrue(currentRouteIs<Route.Browser>())

        navigateTo(Route.MediaHub)
        assertTrue(currentRouteIs<Route.MediaHub>())

        navigateTo(Route.Media(category = MediaCategory.Images.name))
        assertTrue(currentRouteIs<Route.Media>())

        navigateTo(Route.Search())
        assertTrue(currentRouteIs<Route.Search>())

        navigateTo(Route.Settings)
        assertTrue(currentRouteIs<Route.Settings>())
    }

    @Test
    fun settingsAboutActionFiresNavigation() {
        navigateTo(Route.Settings)
        // Primary action: the About row (stable testTag, locale-independent) must
        // drive navigation to the About destination.
        composeRule.onNodeWithTag("open_about").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertTrue(currentRouteIs<Route.About>())
    }

    @Test
    fun homeRendersItsTitle() {
        // A concrete content anchor for the start destination (the app name renders in
        // the Home top bar); complements the route-level checks above.
        composeRule.onNodeWithText("Filora").assertIsDisplayed()
    }

    // --- helpers ---------------------------------------------------------------

    private inline fun <reified T : Any> currentRouteIs(): Boolean =
        navController.currentBackStackEntry?.destination?.hasRoute<T>() == true

    private fun navigateTo(route: Route) {
        composeRule.runOnUiThread { navController.navigate(route) }
        composeRule.waitForIdle()
    }

    private fun popBack() {
        composeRule.runOnUiThread { navController.popBackStack() }
        composeRule.waitForIdle()
    }
}

/** Read permissions to grant for the current API level (mirrors StoragePermissions). */
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
