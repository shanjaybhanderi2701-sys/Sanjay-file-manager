package com.appblish.filora.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
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
 * The critical user journey below is intentionally minimal (launch -> first interactive frame).
 * Extend `collect {}` with the real home/browse interactions once the app is feature-complete
 * (T6.5): scroll the home hubs, open a directory, run a search. Each step that runs here gets
 * its hot classes/methods precompiled.
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
            // TODO(T6.5): once home/browse/search are wired, drive the critical journey here, e.g.
            //   device.findObject(By.res(packageName, "home_hub_grid"))?.fling(Direction.DOWN)
            //   device.waitForIdle()
        }
}
