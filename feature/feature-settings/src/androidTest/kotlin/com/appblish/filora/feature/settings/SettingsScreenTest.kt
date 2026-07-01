package com.appblish.filora.feature.settings

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.appblish.filora.core.domain.model.UserPreferences
import com.appblish.filora.core.ui.theme.FiloraTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * T165 (M16) — screen-level compose test for the Settings screen.
 *
 * Drives the stateless [SettingsContent] with default preferences and asserts the key
 * appearance/about controls render. Assertions key on stable `testTag`s (locale- and
 * copy-independent) rather than on-screen strings.
 */
@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersAppearanceAndAboutControls() {
        composeRule.setContent {
            FiloraTheme {
                SettingsContent(preferences = UserPreferences.Default)
            }
        }
        // FR-11.1 appearance toggle + the About entry point are the screen's key controls.
        composeRule.onNodeWithTag("dynamic_color").assertExists()
        composeRule.onNodeWithTag("open_about").assertExists()
    }
}
