package com.appblish.filora.feature.browser

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.appblish.filora.core.domain.model.FileItem
import com.appblish.filora.core.ui.theme.FiloraTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * T165 (M16) — screen-level Compose UI test for the Browser surface.
 *
 * Renders the stateless [BrowserContent] directly — no Hilt, no navigation, no live
 * data source — so it is deterministic on the emulator matrix. Asserts each phase's key
 * node renders (empty / error / content) and that a row tap fires the primary "open"
 * action. Instrumented (`createComposeRule` on the API30/API35 matrix) per the T165
 * charter. Strings are resolved from resources (not hardcoded) so it stays locale-safe.
 */
@RunWith(AndroidJUnit4::class)
class BrowserContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val resources =
        InstrumentationRegistry.getInstrumentation().targetContext.resources

    private fun setBrowser(
        state: BrowserUiState,
        onItemTap: (FileItem) -> Unit = {},
    ) {
        composeRule.setContent {
            FiloraTheme {
                BrowserContent(
                    uiState = state,
                    onItemTap = onItemTap,
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
    fun emptyPhase_showsEmptyState() {
        setBrowser(BrowserUiState(phase = BrowserUiState.Phase.Empty))
        composeRule
            .onNodeWithText(resources.getString(R.string.browser_empty_title))
            .assertIsDisplayed()
    }

    @Test
    fun errorPhase_showsErrorState() {
        setBrowser(BrowserUiState(phase = BrowserUiState.Phase.Error))
        composeRule
            .onNodeWithText(resources.getString(R.string.browser_error_title))
            .assertIsDisplayed()
    }

    @Test
    fun contentPhase_rendersEntries_andRowTapFiresOpen() {
        val folder =
            FileItem(
                name = "Photos",
                path = "/root/Photos",
                isDirectory = true,
                sizeBytes = 0,
                lastModifiedEpochMillis = 0,
            )
        val file =
            FileItem(
                name = "notes.txt",
                path = "/root/notes.txt",
                isDirectory = false,
                sizeBytes = 12,
                lastModifiedEpochMillis = 0,
            )
        var tapped: FileItem? = null
        setBrowser(
            BrowserUiState(phase = BrowserUiState.Phase.Content, entries = listOf(folder, file)),
            onItemTap = { tapped = it },
        )

        composeRule.onNodeWithText("Photos").assertIsDisplayed()
        composeRule.onNodeWithText("notes.txt").assertIsDisplayed()

        composeRule.onNodeWithText("Photos").performClick()
        assertEquals(folder, tapped)
    }
}
