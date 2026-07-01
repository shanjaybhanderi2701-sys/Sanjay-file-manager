package com.appblish.filora

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.appblish.filora.permission.MediaAccessChecker
import com.appblish.filora.permission.MediaAccessModule
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * Permission-flow instrumentation (T030, FR-1.1): the deny/SAF half of the gate.
 *
 * With **no** media access, the Hilt-wired app must launch into the permission rationale —
 * not Home — and offer both the grant path and the permission-free "continue with limited
 * access" (SAF) escape hatch. This proves the NavHost gating ([MainActivity]) routes a fresh,
 * ungranted first run to onboarding.
 *
 * Test isolation: runtime-permission grants are package-scoped and persist for the whole
 * instrumentation run, so [HomeSmokeTest]'s `GrantPermissionRule` media grant leaks into this
 * test and would flip [MainActivity]'s gate to Home — and revoking in-process crashes the
 * self-instrumented test process. So instead of depending on ambient permission state, this
 * test binds a deterministic [MediaAccessChecker] that reports *no* access, exercising the
 * gate's ungranted branch hermetically regardless of sibling-test order. No test persists a
 * SAF tree, so `hasPersistedTree()` stays false and the gate is shown.
 *
 * [UninstallModules] removes the production [MediaAccessModule] from this test's Hilt graph so
 * the [BindValue] fake is the *sole* [MediaAccessChecker] binding — without it, Hilt sees both
 * the production `@Binds` and the `@BindValue` and fails to compile with `Dagger/DuplicateBindings`.
 *
 * Runs on an emulator/device (`connectedAndroidTest`); it is not a JVM unit test.
 */
@HiltAndroidTest
@UninstallModules(MediaAccessModule::class)
@RunWith(AndroidJUnit4::class)
class PermissionFlowTest {
    private val hiltRule = HiltAndroidRule(this)

    /** Force the ungranted branch of the gate, independent of ambient runtime permissions. */
    @BindValue
    @JvmField
    val mediaAccessChecker: MediaAccessChecker =
        object : MediaAccessChecker {
            override fun hasMediaAccess(): Boolean = false
        }

    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(hiltRule).around(composeRule)

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
