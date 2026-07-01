package com.appblish.filora

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.ui.test.assertDoesNotExist
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

    // --- query form (the routed form for the optional-arg templates) ------------------

    @Test
    fun hostileBrowserDeepLinkFallsBackToHomeInsteadOfBrowsing() {
        assertFallsBackToHome("filora://browser?location=/data/data/com.appblish.filora/databases/filora.db")
    }

    @Test
    fun traversalDeepLinkFallsBackToHome() {
        assertFallsBackToHome("filora://browser?location=/storage/emulated/0/../../data/data/com.appblish.filora")
    }

    // --- path form (what a hostile link uses to try to dodge the query-param check) ----
    // Reviewer F1 repro: with a path-form target, getQueryParameter("location") is null, so
    // an unhardened sanitizer would see the "empty default" and wave the intent through. The
    // sanitizer now also inspects the decoded path tail, so these must still land on Home.

    @Test
    fun pathFormHostileBrowserDeepLinkFallsBackToHome() {
        assertFallsBackToHome("filora://browser/data/data/com.appblish.filora/databases/filora.db")
    }

    @Test
    fun pathFormTraversalDeepLinkFallsBackToHome() {
        assertFallsBackToHome("filora://browser/storage/emulated/0/../../data/data/com.appblish.filora")
    }

    /** Launch [MainActivity] with a crafted VIEW intent and assert it lands on Home, not the target. */
    private fun assertFallsBackToHome(uri: String) {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(uri),
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
