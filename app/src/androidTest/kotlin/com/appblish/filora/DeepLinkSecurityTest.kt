package com.appblish.filora

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * B3 / security-impl-audit F1: firing a hostile exported `filora://` deep link that
 * points at an app-private path must NOT browse it. `ViewIntentValidator` neutralises the
 * intent in [MainActivity], so the app falls back to Home instead of disclosing internal
 * storage — and never crashes.
 *
 * Runs on an emulator/device (`connectedAndroidTest`); it is not a JVM unit test.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class DeepLinkSecurityTest {
    private val hiltRule = HiltAndroidRule(this)

    // Grant media access so the start destination is Home (not the permission gate),
    // making the safe fallback target deterministic.
    private val grantRule = GrantPermissionRule.grant(*mediaReadPermissions())

    // Empty rule: we launch MainActivity ourselves with a crafted intent.
    private val composeRule = createEmptyComposeRule()

    @get:Rule
    val chain: RuleChain =
        RuleChain.outerRule(grantRule).around(hiltRule).around(composeRule)

    @Test
    fun hostileBrowserDeepLinkFallsBackToHomeInsteadOfBrowsing() {
        val hostile = Uri.parse(
            "filora://browser?location=/data/data/com.appblish.filora/databases/filora.db",
        )
        val intent = Intent(
            Intent.ACTION_VIEW,
            hostile,
            ApplicationProvider.getApplicationContext(),
            MainActivity::class.java,
        )

        ActivityScenario.launch<MainActivity>(intent).use {
            composeRule.waitForIdle()

            // Fell back to Home: the Home top bar is shown and the attacker path is not.
            composeRule.onNodeWithText("Filora").assertIsDisplayed()
            composeRule.onNodeWithText("filora.db", substring = true).assertDoesNotExist()
        }
    }

    @Test
    fun traversalDeepLinkFallsBackToHome() {
        val hostile = Uri.parse(
            "filora://browser?location=/storage/emulated/0/../../data/data/com.appblish.filora",
        )
        val intent = Intent(
            Intent.ACTION_VIEW,
            hostile,
            ApplicationProvider.getApplicationContext(),
            MainActivity::class.java,
        )

        ActivityScenario.launch<MainActivity>(intent).use {
            composeRule.waitForIdle()
            composeRule.onNodeWithText("Filora").assertIsDisplayed()
        }
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
