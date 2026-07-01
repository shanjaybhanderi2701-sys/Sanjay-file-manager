package com.appblish.filora.core.ui.screenshot

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.appblish.filora.core.ui.component.EmptyState
import com.appblish.filora.core.ui.component.FileRow
import com.appblish.filora.core.ui.component.GridTile
import com.appblish.filora.core.ui.component.ProgressBarRow
import com.appblish.filora.core.ui.theme.FiloraTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * T171 (M16) — JVM screenshot tests for the shared core-ui components.
 *
 * Roborazzi renders real Compose on the JVM via Robolectric (no emulator), so these run
 * inside the standard `test` / `testDebugUnitTest` unit-test lane. Each component is
 * captured across light / dark / dynamic-color themes and its key states (content /
 * loading / empty / error).
 *
 * Golden images live under `src/test/screenshots/`. Record/refresh them with
 * `./gradlew :core:core-ui:recordRoborazziDebug` and commit the PNGs; CI enforces them
 * with `:core:core-ui:verifyRoborazziDebug` (see docs/phase-1/testing/screenshot-and-coverage.md).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = RobolectricDeviceQualifiers.Pixel5)
class ComponentScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun capture(
        name: String,
        darkTheme: Boolean = false,
        dynamicColor: Boolean = false,
        content: @Composable () -> Unit,
    ) {
        composeRule.setContent {
            FiloraTheme(darkTheme = darkTheme, dynamicColor = dynamicColor) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    content()
                }
            }
        }
        composeRule.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }

    // --- FileRow (content state) ---

    @Test
    fun fileRow_light() =
        capture("file_row_light") {
            SampleFileRow()
        }

    @Test
    fun fileRow_dark() =
        capture("file_row_dark", darkTheme = true) {
            SampleFileRow()
        }

    @Test
    fun fileRow_dynamic() =
        capture("file_row_dynamic", dynamicColor = true) {
            SampleFileRow()
        }

    // --- GridTile (content state) ---

    @Test
    fun gridTile_light() =
        capture("grid_tile_light") {
            SampleGridTile()
        }

    @Test
    fun gridTile_dark() =
        capture("grid_tile_dark", darkTheme = true) {
            SampleGridTile()
        }

    // --- ProgressBarRow (loading state: determinate + indeterminate) ---

    @Test
    fun progressBar_determinate_light() =
        capture("progress_determinate_light") {
            ProgressBarRow(
                label = "Copying files",
                progress = 0.42f,
                detail = "42% · 3 of 7",
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            )
        }

    @Test
    fun progressBar_indeterminate_dark() =
        capture("progress_indeterminate_dark", darkTheme = true) {
            ProgressBarRow(
                label = "Scanning storage",
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            )
        }

    // --- EmptyState (empty + error states) ---

    @Test
    fun emptyState_light() =
        capture("empty_state_light") {
            EmptyState(
                icon = Icons.Outlined.FolderOpen,
                title = "Nothing here yet",
                description = "Files you add will show up here.",
            )
        }

    @Test
    fun emptyState_dark() =
        capture("empty_state_dark", darkTheme = true) {
            EmptyState(
                icon = Icons.Outlined.FolderOpen,
                title = "Nothing here yet",
                description = "Files you add will show up here.",
            )
        }

    @Test
    fun emptyState_dynamic() =
        capture("empty_state_dynamic", dynamicColor = true) {
            EmptyState(
                icon = Icons.Outlined.FolderOpen,
                title = "Nothing here yet",
                description = "Files you add will show up here.",
            )
        }

    @Test
    fun errorState_light() =
        capture("error_state_light") {
            EmptyState(
                icon = Icons.Outlined.ErrorOutline,
                title = "Couldn't load this folder",
                description = "Check the permission and try again.",
            )
        }

    @Composable
    private fun SampleFileRow() {
        FileRow(
            name = "Vacation.png",
            subtitle = "2.4 MB · Jun 30, 2026",
            isDirectory = false,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    @Composable
    private fun SampleGridTile() {
        GridTile(
            label = "Images",
            icon = Icons.Outlined.Image,
            caption = "128 items",
        )
    }
}
