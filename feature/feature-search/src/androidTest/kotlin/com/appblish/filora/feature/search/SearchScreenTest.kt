package com.appblish.filora.feature.search

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.appblish.filora.core.ui.theme.FiloraTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * T165 (M16) — screen-level compose test for the Search screen.
 *
 * Drives the stateless [SearchContent]: asserts the search field renders and that
 * typing into it fires the query-change action (the screen's primary interaction).
 */
@RunWith(AndroidJUnit4::class)
class SearchScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun searchField_rendersAndFiresQueryChange() {
        var lastQuery = ""
        composeRule.setContent {
            FiloraTheme {
                SearchContent(
                    uiState = SearchUiState(),
                    onQueryChange = { lastQuery = it },
                    onToggleType = {},
                    onSelectSize = {},
                    onSelectDate = {},
                    onRemoveChip = {},
                    onOpenResult = {},
                )
            }
        }

        composeRule
            .onNodeWithText(context.getString(R.string.search_hint))
            .assertIsDisplayed()

        composeRule.onNode(hasSetTextAction()).performTextInput("report")
        assertTrue("expected onQueryChange to fire", lastQuery.isNotEmpty())
    }
}
