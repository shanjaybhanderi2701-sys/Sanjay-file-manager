package com.appblish.filora.baselineprofile

import android.content.Intent
import android.net.Uri
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Large-directory performance benchmark (T2.6 — validates NFR-1.2 and NFR-1.3).
 *
 * A directory with [ENTRY_COUNT] entries is browsed and scrolled while [FrameTimingMetric]
 * records per-frame CPU durations. The metric output covers both acceptance targets:
 *
 *  - **NFR-1.2** (10k-entry directory first frame ≤ 300 ms warm / ≤ 800 ms cold) — the first
 *    frames after the deep-link opens the directory reflect the initial render cost. The
 *    directory read + sort run off the main thread ([FrameTimingMetric] ignores that latency,
 *    which is why the cold `StartupTimingMetric` complement in [StartupBenchmark] and the
 *    manual timing note in `docs/phase-1/11-performance-memory-hardening.md` §T2.6 finish the
 *    NFR-1.2 story), so this run's `frameDurationCpuMs` P50 is the render-side budget.
 *  - **NFR-1.3** (scrolling ≥ 58 fps, no sustained jank > 16 ms) — the fling loop below drives
 *    the `LazyColumn`; assert `frameDurationCpuMs` P90/P99 stay ≤ 16 ms (58 fps ⇒ 17.2 ms/frame).
 *
 * Run on a connected device/emulator (also wired into the CI `baseline-profile` job):
 *   ./gradlew :baselineprofile:connectedStandardBenchmarkAndroidTest \
 *       -Pandroid.testInstrumentationRunnerArguments.class=\
 *          com.appblish.filora.baselineprofile.LargeDirectoryBenchmark
 *
 * The fixture is seeded once into the app's own external-files dir (shell-writable under
 * scoped storage and readable by the app without a runtime permission) and the Browser is
 * opened straight onto it via the `filora://browser?location=…` deep link (T053).
 */
@RunWith(AndroidJUnit4::class)
class LargeDirectoryBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun scrollNoCompilation() = scrollLargeDirectory(CompilationMode.None())

    @Test
    fun scrollBaselineProfile() = scrollLargeDirectory(CompilationMode.Partial())

    private fun scrollLargeDirectory(compilationMode: CompilationMode) =
        benchmarkRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = listOf(FrameTimingMetric()),
            iterations = 10,
            // WARM keeps the process alive so we isolate directory-render + scroll cost from
            // cold-start (which StartupBenchmark owns); NFR-1.2's warm budget is the target.
            startupMode = StartupMode.WARM,
            compilationMode = compilationMode,
            setupBlock = {
                // Idempotent: only the first iteration pays the ~10k file-creation cost.
                seedLargeDirectory()
                pressHome()
            },
        ) {
            // Deep-link straight onto the 10k-entry directory so the first measured frames are
            // the large-directory render (NFR-1.2), not the Home dashboard.
            startActivityAndWait(browseIntent(FIXTURE_DIR))
            val list =
                device.wait(Until.findObject(By.res(PACKAGE_NAME, BROWSER_LIST_TAG)), 5_000)
                    ?: device.findObject(By.scrollable(true))
            list?.apply {
                setGestureMargin(device.displayWidth / 5)
                // Several flings each direction to sample sustained scroll jank (NFR-1.3).
                repeat(3) { fling(Direction.DOWN) }
                repeat(3) { fling(Direction.UP) }
            }
            device.waitForIdle()
        }

    /** ACTION_VIEW intent onto a folder via the Browser deep link (`filora://browser?location=`). */
    private fun browseIntent(path: String): Intent =
        Intent(Intent.ACTION_VIEW).apply {
            setPackage(PACKAGE_NAME)
            data = Uri.parse("filora://browser?location=" + Uri.encode(path))
        }

    /**
     * Populates [FIXTURE_DIR] with [ENTRY_COUNT] empty files if not already seeded. Uses a
     * single shell for-loop (toybox `seq`) so the 10k creations run in one round-trip. The
     * path is the app's external-files dir, which the shell can write and the app can read
     * under scoped storage without `MANAGE_EXTERNAL_STORAGE`.
     */
    private fun MacrobenchmarkScope.seedLargeDirectory() {
        val existing =
            device.executeShellCommand("ls -1 $FIXTURE_DIR 2>/dev/null | wc -l").trim().toIntOrNull() ?: 0
        if (existing >= ENTRY_COUNT) return
        device.executeShellCommand("mkdir -p $FIXTURE_DIR")
        device.executeShellCommand(
            "for i in \$(seq 1 $ENTRY_COUNT); do : > $FIXTURE_DIR/entry_\$i.txt; done",
        )
    }

    private companion object {
        const val ENTRY_COUNT = 10_000

        // Compose testTag mirrored from feature-browser BrowserTestTags.LIST (test module can't
        // depend on the feature module). MainActivity exposes tags as resource-ids.
        const val BROWSER_LIST_TAG = "browser_list"

        // The app's own external-files dir on the primary volume — shell-writable, app-readable.
        const val FIXTURE_DIR = "/sdcard/Android/data/$PACKAGE_NAME/files/bench10k"
    }
}
