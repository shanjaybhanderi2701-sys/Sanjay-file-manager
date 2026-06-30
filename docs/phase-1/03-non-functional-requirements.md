# Non-Functional Requirements — Filora

**Status:** Phase 1 — Draft for approval · **Last updated:** 2026-06-30

---

## NFR-1 Performance
- **NFR-1.1** Cold start to interactive home ≤ 1.5 s on mid-tier hardware (e.g. Pixel 6a class).
- **NFR-1.2** A directory with 10,000 entries renders its first frame ≤ 300 ms (warm cache) and ≤ 800 ms cold.
- **NFR-1.3** Scrolling stays ≥ 58 fps (no sustained jank > 16 ms) on the reference device.
- **NFR-1.4** Directory reads, hashing, thumbnailing, and sorting run off the main thread (Coroutines/Dispatchers.IO).

## NFR-2 Reliability & Data Safety
- **NFR-2.1** Crash-free sessions ≥ 99.5% in beta.
- **NFR-2.2** No data loss: move = copy-then-verify-then-delete-source; failures leave source intact.
- **NFR-2.3** Long operations survive process death via WorkManager and resume/report status.
- **NFR-2.4** All destructive actions require confirmation; bulk delete is undoable where the platform allows trash.

## NFR-3 Security & Privacy
- **NFR-3.1** No file content leaves the device. Network use limited to optional, opt-in anonymized diagnostics.
- **NFR-3.2** Least-privilege permissions; broad-storage access (`MANAGE_EXTERNAL_STORAGE`) only with explicit rationale and Play-policy justification.
- **NFR-3.3** Shared files exposed only via `FileProvider` with scoped, time-bounded grants.
- **NFR-3.4** No secrets in source; no analytics SDK that exfiltrates file names/paths.

## NFR-4 Compatibility
- **NFR-4.1** Min SDK 26, target latest stable SDK; verified on API 26, 30, 33, and latest.
- **NFR-4.2** Correct behavior across scoped-storage transitions (pre/post API 29, READ_MEDIA_* on 33+, partial media access on 34+).
- **NFR-4.3** Phone and tablet layouts; portrait and landscape; supports configuration changes without state loss.

## NFR-5 Accessibility
- **NFR-5.1** All interactive elements have content descriptions; TalkBack-navigable.
- **NFR-5.2** Meets WCAG AA contrast in light and dark themes; respects system font scale up to 200%.
- **NFR-5.3** Minimum touch target 48dp.

## NFR-6 Maintainability & Quality
- **NFR-6.1** Clean Architecture layering enforced (domain has no Android framework deps).
- **NFR-6.2** Domain/use-case layer ≥ 80% unit-test coverage; critical operation logic ≥ 90%.
- **NFR-6.3** Static analysis (ktlint + detekt) passes in CI with zero new violations.
- **NFR-6.4** Every PR builds, lints, and runs unit tests in CI before merge.

## NFR-7 Localization & Internationalization
- **NFR-7.1** All user-facing strings externalized to resources; no hardcoded UI text.
- **NFR-7.2** RTL layout support; locale-aware date/size formatting.

## NFR-8 Observability
- **NFR-8.1** Structured local logging with levels; no PII/file-path leakage at default level.
- **NFR-8.2** Optional opt-in crash reporting; clearly disclosed and disableable.

## NFR-9 App Size & Resource Use
- **NFR-9.1** Release APK/AAB base ≤ 12 MB (excluding dynamic features); R8/ProGuard enabled.
- **NFR-9.2** No memory leaks on navigation (verified via LeakCanary in debug); bounded thumbnail cache.
