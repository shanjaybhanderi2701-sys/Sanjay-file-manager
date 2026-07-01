package com.appblish.filora.feature.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.appblish.filora.core.domain.model.UserPreferences
import com.appblish.filora.core.ui.theme.FiloraTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * T165 (M16) — screen-level compose test for the Settings screen.
 *
 * Drives the stateless [SettingsContent] with default preferences and asserts the
 * appearance section renders (FR-11.1).
 */
@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun rendersAppearanceSection() {
        composeRule.setContent {
            FiloraTheme {
                SettingsContent(preferences = UserPreferences.Default)
            }
        }
        composeRule
            .onNodeWithText(context.getString(R.string.settings_section_appearance))
            .assertIsDisplayed()
    }
}
