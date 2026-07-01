package com.appblish.filora

import android.Manifest
import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * T165 — screen-level Compose UI tests driven through the real app
 * ([createAndroidComposeRule]<[MainActivity]>), exercising the destinations that have
 * a real in-app UI entry point today: Home and Settings. Full-graph reachability of
 * the other key screens (Browser, media hub, Search) is covered route-first in
 * [NavGraphNavigationTest]; deeper per-node assertions for those await stable
 * `testTag`s on their content (tracked as a follow-up).
 *
 * Assertions prefer locale-independent `testTag`s where the screen exposes them
 * (Settings) and fall back to the same default-locale copy the existing
 * [HomeSmokeTest]/[PermissionFlowTest] assert on.
 *
 * Runs on an emulator/device (`connectedStandardDebugAndroidTest`). Authored where no
 * JDK/emulator is available; CI-green observed on the instrumented-matrix lane post-merge.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class KeyScreensRenderTest {
    private val hiltRule = HiltAndroidRule(this)
    private val grantRule = GrantPermissionRule.grant(*mediaReadPermissions())
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val chain: RuleChain =
        RuleChain.outerRule(grantRule).around(hiltRule).around(composeRule)

    @Test
    fun homeScreenRendersDashboardChrome() {
        composeRule.waitForIdle()

        // Home renders (past the permission gate, access granted) with its top bar…
        composeRule.onNodeWithText("Filora").assertIsDisplayed()
        // …and the Settings entry-point action that hands off to the Settings screen.
        composeRule.onNodeWithContentDescription("Settings").assertIsDisplayed()
    }

    @Test
    fun settingsScreenRendersItsControls() {
        composeRule.waitForIdle()

        // Primary action from Home: open Settings.
        composeRule.onNodeWithContentDescription("Settings").performClick()
        composeRule.waitForIdle()

        // The Settings surface renders its stable, tagged controls (FR-10 preferences).
        composeRule.onNodeWithTag("dynamic_color").assertIsDisplayed()
        composeRule.onNodeWithTag("show_hidden").assertIsDisplayed()
        composeRule.onNodeWithTag("open_about").assertIsDisplayed()
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
