package com.appblish.filora.feature.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.appblish.filora.core.ui.theme.FiloraTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * T165 (M16) — screen-level compose test for the Home dashboard.
 *
 * Drives the stateless [HomeContent] directly. A default [HomeUiState] resolves to
 * the "welcome / empty" state (access granted, nothing to show yet), which is the
 * key zero-data surface to assert renders.
 */
@RunWith(AndroidJUnit4::class)
class HomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun emptyState_showsWelcomeTitle() {
        composeRule.setContent {
            FiloraTheme {
                HomeContent(
                    uiState = HomeUiState(),
                    onOpenCategory = {},
                    onBrowse = {},
                )
            }
        }
        composeRule
            .onNodeWithText(context.getString(R.string.home_welcome_title))
            .assertIsDisplayed()
    }

    @Test
    fun permissionRequired_showsPermissionPrompt() {
        composeRule.setContent {
            FiloraTheme {
                HomeContent(
                    uiState = HomeUiState(permissionRequired = true),
                    onOpenCategory = {},
                    onBrowse = {},
                )
            }
        }
        composeRule
            .onNodeWithText(context.getString(R.string.home_permission_title))
            .assertIsDisplayed()
    }
}
