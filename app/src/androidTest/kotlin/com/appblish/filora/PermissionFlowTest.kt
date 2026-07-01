package com.appblish.filora

import android.Manifest
import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * Permission-flow instrumentation (T030, FR-1.1): the deny/SAF half of the gate.
 *
 * With **no** media permission granted and no persisted SAF tree, the Hilt-wired app
 * must launch into the permission rationale — not Home — and offer both the grant path
 * and the permission-free "continue with limited access" (SAF) escape hatch. This proves
 * the NavHost gating ([MainActivity]) routes a fresh, ungranted first run to onboarding.
 *
 * Test isolation: runtime-permission grants are **package-scoped and persist for the whole
 * instrumentation run** — they are never auto-revoked between test classes. [HomeSmokeTest]
 * grants the `READ_MEDIA_*` permissions via `GrantPermissionRule`, so whichever order the
 * classes run in, this test can inherit a still-granted package. That flips
 * [MainActivity]'s synchronous `hasMediaAccess()` gate to Home and the rationale never
 * renders. So this test explicitly re-establishes its own precondition by **revoking** the
 * read permissions before the activity is launched (the `revokeReadPermissions` rule sits
 * outside `composeRule`, whose `ActivityScenario` cold-starts [MainActivity] afterwards).
 *
 * Runs on an emulator/device (`connectedAndroidTest`); it is not a JVM unit test.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class PermissionFlowTest {
    private val hiltRule = HiltAndroidRule(this)

    // Must revoke *before* composeRule's ActivityScenario launches MainActivity, so the
    // activity's onCreate observes the ungranted state and gates to the rationale.
    private val revokeReadPermissions =
        object : ExternalResource() {
            override fun before() {
                val instrumentation = InstrumentationRegistry.getInstrumentation()
                val packageName = instrumentation.targetContext.packageName
                revocableReadPermissions().forEach { permission ->
                    // No-op if the package never held it; otherwise the platform revokes it
                    // (killing the target process if alive) so the next launch is a clean,
                    // ungranted cold start.
                    runCatching {
                        instrumentation.uiAutomation.revokeRuntimePermission(packageName, permission)
                    }
                }
            }
        }

    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val chain: RuleChain =
        RuleChain.outerRule(revokeReadPermissions).around(hiltRule).around(composeRule)

    @Test
    fun freshLaunchWithoutAccessShowsPermissionGate() {
        composeRule.waitForIdle()

        // The rationale, not Home: the gate explains why and offers to grant.
        // The gate's Column is `verticalScroll`-able, so on shorter viewports (e.g. the
        // API30 phone and tablet targets) the lower actions render below the fold.
        // Scroll each node into view before asserting visibility — `assertIsDisplayed`
        // does not auto-scroll, so a present-but-clipped node would otherwise fail.
        composeRule.onNodeWithText("Access your files").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Grant access").performScrollTo().assertIsDisplayed()
        // The permission-free SAF path is always reachable — never a dead end.
        composeRule.onNodeWithText("Continue with limited access").performScrollTo().assertIsDisplayed()
    }
}

/** The read permissions to clear so the gate sees a fresh, ungranted launch. */
private fun revocableReadPermissions(): List<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        buildList {
            add(Manifest.permission.READ_MEDIA_IMAGES)
            add(Manifest.permission.READ_MEDIA_VIDEO)
            add(Manifest.permission.READ_MEDIA_AUDIO)
            // API 34+ partial-media ("selected photos") grant also counts as access.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                add(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
            }
        }
    } else {
        listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
