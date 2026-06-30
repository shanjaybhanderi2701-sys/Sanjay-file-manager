package com.appblish.filora

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * Permission-flow instrumentation (T030, FR-1.1): the deny/SAF half of the gate.
 *
 * With **no** media permission granted (note the absence of [androidx.test.rule.GrantPermissionRule],
 * unlike [HomeSmokeTest]) and no persisted SAF tree, the Hilt-wired app must launch
 * into the permission rationale — not Home — and offer both the grant path and the
 * permission-free "continue with limited access" (SAF) escape hatch. This proves the
 * NavHost gating ([MainActivity]) routes a fresh, ungranted first run to onboarding.
 *
 * Runs on an emulator/device (`connectedAndroidTest`); it is not a JVM unit test.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class PermissionFlowTest {
    private val hiltRule = HiltAndroidRule(this)
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(hiltRule).around(composeRule)

    @Test
    fun freshLaunchWithoutAccessShowsPermissionGate() {
        composeRule.waitForIdle()

        // The rationale, not Home: the gate explains why and offers to grant.
        composeRule.onNodeWithText("Access your files").assertIsDisplayed()
        composeRule.onNodeWithText("Grant access").assertIsDisplayed()
        // The permission-free SAF path is always reachable — never a dead end.
        composeRule.onNodeWithText("Continue with limited access").assertIsDisplayed()
    }
}
