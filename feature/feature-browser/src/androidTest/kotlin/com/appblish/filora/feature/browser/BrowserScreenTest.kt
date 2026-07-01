package com.appblish.filora.feature.browser

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
 * T165 (M16) — screen-level compose test for the Browser screen.
 *
 * Drives the stateless [BrowserContent] directly (no Hilt/ViewModel) so each visual
 * state is asserted in isolation on the emulator matrix. Assertions resolve strings
 * via the resource id, keeping the test locale-independent.
 */
@RunWith(AndroidJUnit4::class)
class BrowserScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun setContent(uiState: BrowserUiState) {
        composeRule.setContent {
            FiloraTheme {
                BrowserContent(
                    uiState = uiState,
                    onItemTap = {},
                    onToggleSelection = {},
                    onToggleFavorite = {},
                    onToggleLayout = {},
                    onSortBy = {},
                    onToggleHidden = {},
                    onRefresh = {},
                )
            }
        }
    }

    @Test
    fun emptyState_showsEmptyTitle() {
        setContent(BrowserUiState(phase = BrowserUiState.Phase.Empty))
        composeRule
            .onNodeWithText(context.getString(R.string.browser_empty_title))
            .assertIsDisplayed()
    }

    @Test
    fun errorState_showsErrorTitle() {
        setContent(BrowserUiState(phase = BrowserUiState.Phase.Error))
        composeRule
            .onNodeWithText(context.getString(R.string.browser_error_title))
            .assertIsDisplayed()
    }
}
