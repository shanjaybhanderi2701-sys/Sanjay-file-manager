# M7 / T7.4 — Performance & Memory Hardening (NFR-1, NFR-9)

Owner: Founding Android Engineer · Parent: APP-46 (M7) · Issue: APP-84

This runbook records the hardening **infrastructure** (landed now, additive, non-gated) and
the **on-device validation** that closes NFR-1 / NFR-9.2 (gated on feature-complete, T6.5).

## Targets

| NFR | Requirement | How it is met |
| --- | --- | --- |
| NFR-1.1 | Cold start to interactive home ≤ 1.5 s (Pixel 6a class) | Baseline profile (`:baselineprofile`) + ProfileInstaller; measured by `StartupBenchmark`. |
| NFR-1.2 | 10k-entry directory first frame ≤ 300 ms warm / ≤ 800 ms cold | Lazy paging + off-main-thread reads (NFR-1.4); validate via Macrobenchmark frame timing once browse is feature-complete. |
| NFR-1.3 | Scrolling ≥ 58 fps, no sustained jank > 16 ms | `FrameTimingMetric` jank run on the browse/media lists. |
| NFR-1.4 | Reads / hashing / thumbnailing / sorting off main thread | Dispatchers.IO via `DispatcherModule`; already enforced in use cases. |
| NFR-9.1 | Release base ≤ 12 MB, R8 enabled | `isMinifyEnabled`/`isShrinkResources` already on in `:app` release; check AAB size in CI. |
| NFR-9.2 | No leaks on navigation; bounded thumbnail cache | LeakCanary (debug) + bounded LRU cache (T4.2, `BoundedLruCache`). |

## Landed in this issue (infrastructure — not gated)

- **LeakCanary** wired as `debugImplementation` in `:app`. Auto-installs via its ContentProvider;
  watches every destroyed Activity/Fragment/ViewModel and dumps a heap analysis on a retained
  instance. No production code or init call. (NFR-9.2)
- **ProfileInstaller** added to `:app` so the baked baseline profile is applied at first run. (NFR-1.1)
- **`androidx.baselineprofile` plugin** on `:app` (consumer) — bakes the generated profile into the AAB.
- **`:baselineprofile`** `com.android.test` module (producer):
  - `BaselineProfileGenerator` — captures the cold-start critical journey.
  - `StartupBenchmark` — cold-start timing, no-compilation vs baseline-profile, gated against 1.5 s.
- Version catalog entries for all of the above (leakcanary, profileinstaller, baseline-profile,
  benchmark-macro, uiautomator, test-runner).
- Bounded thumbnail cache (NFR-9.2 second clause) already satisfied by `BoundedLruCache` from T4.2.

## On-device validation (GATED on T6.5 feature-complete)

These steps need a connected device/emulator AND a launchable, feature-complete app. They are the
acceptance evidence for closing APP-84; run them once T6.5 lands and attach results.

1. **LeakCanary clean (NFR-9.2)** — install the debug build, exercise every navigation path
   (home → browse → open dir → search → media hubs → storage → settings, plus multi-select and
   file operations), background/rotate at each. Acceptance: **zero** retained-instance leaks reported.
   Fix any leak (common culprits: ViewModel-held Context, un-cancelled coroutine scope, registered
   listeners not removed in `onCleared`/`DisposableEffect`).
2. **Baseline profile (NFR-1.1)** — extend `BaselineProfileGenerator.generate {}` with the real
   critical journey, then:
   `./gradlew :baselineprofile:generateBaselineProfile`
   Commit the generated `app/src/standard/generated/baselineProfiles/baseline-prof.txt`.
3. **Startup budget (NFR-1.1)** — `./gradlew :baselineprofile:connectedBenchmarkAndroidTest`
   Compare `startupBaselineProfile` `timeToInitialDisplayMs` against the 1.5 s budget on a
   Pixel 6a-class device.
4. **Scroll jank (NFR-1.3)** and **large-directory first frame (NFR-1.2)** — add a
   `FrameTimingMetric` macrobenchmark over the browse/media lists with a 10k-entry fixture.
5. **APK/AAB size (NFR-9.1)** — `./gradlew :app:bundleStandardRelease`; assert base module ≤ 12 MB.

## Unblock owner / action

Blocked on **T6.5 (feature-complete)**. Unblock owner: whoever lands T6.5. Action: once the app is
feature-complete and launchable, run steps 1–5 above, attach LeakCanary results + benchmark numbers
+ the committed baseline profile, then close APP-84.
