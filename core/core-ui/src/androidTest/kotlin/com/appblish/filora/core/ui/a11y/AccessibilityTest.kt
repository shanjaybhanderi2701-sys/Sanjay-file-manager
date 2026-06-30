package com.appblish.filora.core.ui.a11y

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.appblish.filora.core.ui.component.ProgressBarRow
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Instrumented checks for the NFR-5 (M15) accessibility primitives. These assert the
 * exact semantics TalkBack reads — control role, spoken action label, minimum touch
 * target, and live-region announcement — so the behaviour is regression-locked instead
 * of relying on a one-off manual sweep.
 */
class AccessibilityTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun clickableTile_exposesButtonRole_clickLabel_andMinTouchTarget() {
        var clicks = 0
        composeRule.setContent {
            Box(
                Modifier
                    .testTag("tile")
                    .clickableTile(onClickLabel = "open Images") { clicks++ },
            ) {
                Text("Images")
            }
        }

        val node = composeRule.onNodeWithTag("tile")

        // Announced as a Button, not static text.
        node.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))

        // The spoken action is the supplied label ("double tap to open Images").
        node.assert(
            SemanticsMatcher("onClick label == 'open Images'") {
                it.config.getOrNull(SemanticsActions.OnClick)?.label == "open Images"
            },
        )

        // WCAG 2.5.5 / Material minimum target even though the text is one short line.
        node.assertHeightIsAtLeast(MinTouchTargetSize)

        node.performClick()
        assertTrue("clickableTile should invoke onClick", clicks == 1)
    }

    @Test
    fun progressBarRow_isPoliteLiveRegion() {
        composeRule.setContent {
            ProgressBarRow(
                label = "Copying",
                progress = 0.5f,
                detail = "report.pdf",
                modifier = Modifier.testTag("progress"),
            )
        }

        composeRule.onNodeWithTag("progress").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite),
        )
    }
}
