package com.appblish.filora.feature.media

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.appblish.filora.core.domain.model.MediaCategory
import com.appblish.filora.core.ui.theme.FiloraTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * T165 (M16) — screen-level compose test for the Media category hub.
 *
 * Drives the stateless [MediaCategoryContent] with the full seven-hub tile list and
 * asserts the grid renders and that tapping a tile fires the open-category action.
 */
@RunWith(AndroidJUnit4::class)
class MediaHubScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun loadedHub_rendersTiles_andTapOpensCategory() {
        var opened: MediaCategory? = null
        composeRule.setContent {
            FiloraTheme {
                MediaCategoryContent(
                    uiState =
                        MediaHubUiState(
                            isLoading = false,
                            tiles = buildHubTiles(emptyMap()),
                        ),
                    onOpenCategory = { opened = it },
                )
            }
        }

        val imagesLabel = context.getString(R.string.media_hub_images)
        composeRule.onNodeWithText(imagesLabel).assertIsDisplayed()

        composeRule.onNodeWithText(imagesLabel).performClick()
        assertEquals(MediaCategory.Images, opened)
    }
}
