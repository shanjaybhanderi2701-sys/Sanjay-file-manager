package com.appblish.filora.baselineprofile

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Measures cold-start time to verify NFR-1.1 (cold start to interactive home ≤ 1.5 s on a
 * Pixel 6a-class device) and to quantify the baseline-profile win.
 *
 * Run on a connected device/emulator:
 *   ./gradlew :baselineprofile:connectedBenchmarkAndroidTest
 *
 * Two variants run so the profile's effect is visible:
 *  - [startupNoCompilation]      cold start with no AOT (worst case).
 *  - [startupBaselineProfile]    cold start with the generated baseline profile applied.
 *
 * Compare `timeToInitialDisplayMs` between the two; the baseline-profile run should be the one
 * gated against the 1.5 s budget.
 */
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun startupNoCompilation() = startup(CompilationMode.None())

    @Test
    fun startupBaselineProfile() =
        startup(CompilationMode.Partial(baselineProfileMode = BaselineProfileMode.Require))

    private fun startup(compilationMode: CompilationMode) = benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(StartupTimingMetric()),
        iterations = 10,
        startupMode = StartupMode.COLD,
        compilationMode = compilationMode,
    ) {
        pressHome()
        startActivityAndWait()
    }
}
