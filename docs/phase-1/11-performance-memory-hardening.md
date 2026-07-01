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
2. **Baseline profile (NFR-1.1)** — the Home critical journey is now wired in
   `BaselineProfileGenerator.generate {}` (launch → Home → scroll dashboard). Generate with:
   `./gradlew :baselineprofile:generateBaselineProfile`
   Commit the generated `app/src/standard/generated/baselineProfiles/baseline-prof.txt`.
3. **Startup budget (NFR-1.1)** — `./gradlew :baselineprofile:connectedStandardBenchmarkAndroidTest`
   Compare `startupBaselineProfile` `timeToInitialDisplayMs` against the 1.5 s budget on a
   Pixel 6a-class device.
4. **Scroll jank (NFR-1.3)** and **large-directory first frame (NFR-1.2)** — add a
   `FrameTimingMetric` macrobenchmark over the browse/media lists with a 10k-entry fixture.
5. **APK/AAB size (NFR-9.1)** — `./gradlew :app:bundleStandardRelease`; assert base module ≤ 12 MB.

## Automation (CI)

Steps 2–3 are automated by the **`baseline-profile`** job in `.github/workflows/ci.yml`: on push to
`main` (or manual `workflow_dispatch`) it boots an API-34 `pixel_6` emulator
(`reactivecircus/android-emulator-runner`), runs `:baselineprofile:generateBaselineProfile` and the
cold-start macrobenchmark, and uploads the generated profile + benchmark results as artifacts. This
is the agent-owned, reproducible path to the on-device NFR-1.1 evidence — it executes once the repo
has GitHub Actions write access (the standing org-level CI blocker, APP-52). Step 1 (LeakCanary) and
step 4 (jank/10k-frame) still need a manual/instrumented device pass.

## Status

- **Gate lifted:** T6.5 (feature-complete, APP-80) is **done** — the dependency is resolved.
- **Landed:** hardening infra + Home baseline-profile journey + `testTagsAsResourceId` + CI emulator
  job. All turnkey.
- **Remaining (needs execution hardware):** run the `baseline-profile` CI job (or a local device) to
  capture the LeakCanary-clean sweep, the committed baseline profile, and the ≤ 1.5 s cold-start
  number, then attach them and close APP-84. This requires a CI Android-emulator runner, gated on the
  GitHub Actions write-access blocker (APP-52 / the board GitHub-access approval). No device/emulator
  is available in the headless agent workspace.

---

## T2.6 — Large-directory performance (APP-62, NFR-1.2 / NFR-1.3)

Owner: Founding Android Engineer · Parent: APP-41 (M2) · Dep: T2.2 (browse listing, resolved).
**AC:** NFR-1.2 and NFR-1.3 met on a 10,000-entry directory.

### How the targets are met (in the browse code)

- **NFR-1.2 (first frame ≤ 300 ms warm / ≤ 800 ms cold).** The directory read *and* sort run off
  the main thread — `FileRepositoryImpl.listDirectory` streams on a cold `flow { … }.flowOn(ioDispatcher)`
  and `ordered(sortOrder)` sorts inside that flow (NFR-1.4). The Browser shows a loading state until the
  snapshot arrives, so the main thread never blocks on the 10k `stat` walk. Rendering is a `LazyColumn` /
  `LazyVerticalGrid`, so only the ~visible rows compose on first frame regardless of directory size.
- **NFR-1.3 (scroll ≥ 58 fps, no sustained jank > 16 ms).** Both lists key each item by
  `FileItem::path` (`items(entries, key = FileItem::path)`), so Compose reuses row nodes across scroll
  instead of re-laying-out the window on every fling. Row content (`FileRow` / `GridTile`) is flat and
  allocation-light; subtitles format lazily per visible row only.

### Acceptance harness (landed — `:baselineprofile:LargeDirectoryBenchmark`)

`LargeDirectoryBenchmark` seeds a **10,000-entry** directory once into the app's external-files dir
(`/sdcard/Android/data/<pkg>/files/bench10k` — shell-writable, app-readable under scoped storage),
deep-links the Browser straight onto it (`filora://browser?location=…`), and scrolls it under
`FrameTimingMetric`:

- `frameDurationCpuMs` **P50** is the render-side NFR-1.2 budget (pair with `StartupBenchmark` for the
  cold end-to-end number).
- `frameDurationCpuMs` **P90 / P99 ≤ 16 ms** (58 fps ⇒ 17.2 ms/frame) is the NFR-1.3 jank gate.

The `LazyColumn` / `LazyVerticalGrid` carry `testTag`s (`browser_list` / `browser_grid`, exposed as
resource-ids by `MainActivity`) so the benchmark addresses the scroll container deterministically.

Run it (already covered by the CI `baseline-profile` job's `connectedStandardBenchmarkAndroidTest`,
which runs *every* benchmark class in the module — no workflow change needed):

```
./gradlew :baselineprofile:connectedStandardBenchmarkAndroidTest \
    -Pandroid.testInstrumentationRunnerArguments.class=com.appblish.filora.baselineprofile.LargeDirectoryBenchmark
```

**Remaining (needs execution hardware):** capture the `frameDurationCpuMs` percentiles from the CI
emulator run and assert them against the budgets above. Same gate as the rest of NFR-1 on-device
validation — the CI Android-emulator runner behind the GitHub Actions write-access blocker
(APP-52). No device/emulator is available in the headless agent workspace.

---

## T7.5 — R8 / minify + release config (APP-85)

Owner: Founding Android Engineer · Parent: APP-46 (M7) · Dep: T7.4 (APP-84).
**AC:** NFR-9.1 size budget; non-debuggable release.

Landed in `:app` (config-only, no app code change):

- **Non-debuggable release.** `buildTypes.release` pins `isDebuggable = false` explicitly so a
  later edit cannot silently ship a debuggable build. Pairs with `isMinifyEnabled = true` +
  `isShrinkResources = true` (R8 code + resource shrink) for the NFR-9.1 budget.
- **R8 full mode** pinned in `gradle.properties` (`android.enableR8.fullMode=true`) — AGP 8 default,
  made explicit so the budget can't regress on an AGP bump.
- **R8 keep rules** (`app/proguard-rules.pro`) — deliberately minimal; library consumer rules cover
  most reflection. App-specific keeps added: WorkManager workers (instantiated by the **default**
  factory via reflection on the class name in `WorkData` — would crash without a keep), Hilt
  `@EntryPoint` interfaces resolved at runtime, `@Serializable` nav-route/domain classes, domain
  enum `values()/valueOf()`, and `SourceFile,LineNumberTable` for readable Play Console traces.
- **CI-safe release signing.** `signingConfigs.release` reads keystore path/passwords/alias from
  gradle properties or the `FILORA_RELEASE_*` env vars; when unset it falls back to the debug
  keystore via `initWith(getByName("debug"))`, so `assembleStandardRelease` / `bundleStandardRelease`
  always build (PR CI, local dev) without committing secrets. Production signing is supplied at
  release time via those env vars (or Play App Signing on the uploaded AAB).
- **Size-budget gate (NFR-9.1).** `./gradlew :app:verifyStandardReleaseSizeBudget` builds the
  minified standard release APK and hard-fails if it exceeds **12 MB**. Wired into CI as the
  dedicated `release-size` job, which also uploads the APK + R8 `mapping.txt` as artifacts.

Production signing env vars (set as CI secrets at release time):

```
FILORA_RELEASE_STORE_FILE       # path to the upload keystore
FILORA_RELEASE_STORE_PASSWORD
FILORA_RELEASE_KEY_ALIAS
FILORA_RELEASE_KEY_PASSWORD
```

Verify locally / in CI:

```
./gradlew :app:verifyStandardReleaseSizeBudget   # R8 build + 12 MB assertion
./gradlew :app:bundleStandardRelease             # the AAB uploaded to Play
```

> On-device NFR-9.1 acceptance numbers (actual MB) are captured under M7 step 5 above once the app
> is feature-complete (gated on T6.5); this issue lands the **enforced config + CI gate**.
