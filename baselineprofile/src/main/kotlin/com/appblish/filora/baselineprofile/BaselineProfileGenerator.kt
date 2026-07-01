package com.appblish.filora.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Generates the baseline profile for Filora (NFR-1.1: faster cold start to interactive home).
 *
 * Run on a connected device/emulator:
 *   ./gradlew :baselineprofile:generateBaselineProfile
 *
 * The resulting `baseline-prof.txt` is copied into `:app/src/<flavor>/generated/baselineProfiles/`
 * by the AndroidX Baseline Profile plugin and baked into the release AAB.
 *
 * The critical user journey drives launch -> Home -> scroll the dashboard, so the hot Compose
 * classes/methods for the startup surface (Home hubs, volume cards, recents/favorites strips)
 * get precompiled. MainActivity exposes Compose `testTag`s as resource-ids, so nodes are
 * addressable via `By.res(packageName, tag)`.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generate() =
        baselineProfileRule.collect(
            packageName = PACKAGE_NAME,
            // Stable, since the home screen content is data-dependent across devices.
            includeInStartupProfile = true,
        ) {
            pressHome()
            startActivityAndWait()
            // Wait for the app content to be present, then scroll the dashboard so the Home
            // list composables are exercised (and thus included in the profile).
            device.wait(Until.hasObject(By.res(PACKAGE_NAME, "filora_app_root")), 5_000)
            device.findObject(By.scrollable(true))?.apply {
                setGestureMargin(device.displayWidth / 5)
                fling(Direction.DOWN)
                fling(Direction.UP)
            }
            device.waitForIdle()
        }
}
