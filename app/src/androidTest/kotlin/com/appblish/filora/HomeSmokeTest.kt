package com.appblish.filora

import android.Manifest
import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * M4 exit-criterion smoke (spec §6): with media access granted, the Hilt-wired app
 * launches end-to-end, clears the permission gate, and renders the Home dashboard
 * — never the permission-required prompt. Proves the T4.1–T4.5 surfaces integrate
 * behind the real navigation graph and DI graph, not just in isolation.
 *
 * Runs on an emulator/device (`connectedAndroidTest`); it is not a JVM unit test.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class HomeSmokeTest {
    private val hiltRule = HiltAndroidRule(this)
    private val grantRule = GrantPermissionRule.grant(*mediaReadPermissions())
    private val composeRule = createAndroidComposeRule<MainActivity>()

    // Grant + Hilt must take effect before the activity launches.
    @get:Rule
    val chain: RuleChain =
        RuleChain.outerRule(grantRule).around(hiltRule).around(composeRule)

    @Test
    fun appLaunchesIntoHomeWithAccessGranted() {
        composeRule.waitForIdle()

        // Home top bar proves we launched past the permission gate end-to-end.
        composeRule.onNodeWithText("Filora").assertIsDisplayed()
        // With access granted the permission-aware branch must not show its prompt.
        composeRule.onNodeWithText("Grant storage access").assertDoesNotExist()
    }
}

/** The read permissions to grant for the current API level (mirrors StoragePermissions). */
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
