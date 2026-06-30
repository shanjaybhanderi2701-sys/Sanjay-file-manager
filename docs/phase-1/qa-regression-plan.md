# QA Regression Plan — Filora v1 (T7.6 / APP-86)

**Status:** Phase 1 — Ready for execution · **Last updated:** 2026-06-30
**Owner (authoring):** Founding Android Engineer · **Owner (execution):** QA Engineer
**AC:** 0 open P0/P1 defects; NFR-4 compatibility matrix pass.
**Depends on (all landed):** T7.2 Accessibility (APP-82), T7.3 Localization/RTL (APP-83),
T7.4 Perf/memory (APP-84), T7.5 R8/release config (APP-85).

This plan is the authoritative regression charter for the v1 GA quality gate
(Release Plan §5). It is written to be executed by the QA Engineer with no
further design input: every charter maps to a Functional Requirement (FR) or
Non-Functional Requirement (NFR), states preconditions, steps, and the
pass condition, and the device matrix enumerates every NFR-4 cell.

---

## 1. Build under test

- **Branch / commit:** `main` @ `e70b472` (or the latest green `main` at execution time).
- **Variants:**
  - **`standardDebug`** — primary functional regression (LeakCanary + StrictMode active).
  - **`standardRelease`** — R8/minified, non-debuggable; used for the *release-sanity*
    suite (§6.K) to catch keep-rule / reflection regressions (WorkManager workers,
    Room, Hilt). Signed with `FILORA_RELEASE_*` or debug-fallback per APP-85.
- **Install source:** locally assembled AAB→APKs via bundletool, or `installStandardDebug`.
- **Pre-flight (must be green before manual run starts):**
  - `./gradlew testStandardDebugUnitTest` — all unit suites pass.
  - `./gradlew ktlintCheck detekt` — zero violations (NFR-6.3).
  - `./gradlew :core:core-ui:connectedStandardDebugAndroidTest` — instrumented a11y
    smoke (AccessibilityTest) green (already verified on API 35, APP-82).
  - `./gradlew :app:assembleStandardRelease` + `verifyStandardReleaseSizeBudget`
    — release builds and is ≤ 12 MB (NFR-9.1, APP-85).

If any pre-flight fails, **stop** and file against the owning task; do not start the
manual matrix on a red build.

---

## 2. NFR-4 device matrix

NFR-4.1 names **API 26, 30, 33, latest**; NFR-4.3 names **phone + tablet, portrait +
landscape, config-change survival**; NFR-4.2 names **scoped-storage transitions**. The
matrix below covers every required cell. AVDs are acceptable for all rows; at least one
**physical device** row (R5) is required for real removable-storage / share-target /
thumbnail-decode coverage.

| Row | Device class | API / OS | Form factor | Storage model under test (NFR-4.2) | Required orientation runs |
|-----|--------------|----------|-------------|------------------------------------|---------------------------|
| R1 | Phone (AVD)  | **API 26** (8.0) | Phone | Legacy pre-scoped storage; `READ/WRITE_EXTERNAL_STORAGE` broad grant | Portrait + Landscape |
| R2 | Phone (AVD)  | **API 30** (11)  | Phone | Scoped storage enforced; SAF for tree access | Portrait + Landscape |
| R3 | Phone (AVD)  | **API 33** (13)  | Phone | `READ_MEDIA_IMAGES/VIDEO/AUDIO` granular media perms | Portrait + Landscape |
| R4 | Phone (AVD)  | **API 35** (latest) | Phone | Granular media + **partial media access** (selected photos) | Portrait + Landscape |
| R5 | Phone (physical) | API 33 or 35 | Phone | Real removable SD / USB-OTG (FR-1.2), real share targets | Portrait + Landscape |
| R6 | Tablet (AVD) | **API 33** (13)  | Tablet (≥600dp) | Granular media perms; large-screen layout (NFR-4.3) | Portrait + Landscape |
| R7 | Tablet (AVD) | **API 35** (latest) | Tablet | Partial media access; multi-window/resizable | Portrait + Landscape |

**Run economics.** Not every functional charter runs on every row. Apply tiers:

- **Full regression (all §6 suites A–K):** R3 (phone, modern baseline) + R6 (tablet).
- **Compatibility regression (A–F + I + K — perms, browse, ops, media, search, settings,
  release-sanity):** R1, R2, R4, R7.
- **Storage-reality pass (A storage/volumes, C ops to SD/OTG, J share to real apps):** R5.
- **Config-change pass (NFR-4.3 rotation/state across A–H):** every row, both orientations.

Total = 7 rows × ~the suites above. Track per-cell pass/fail in §8 grid.

---

## 3. Severity definitions & exit criteria

| Sev | Definition | Examples |
|-----|------------|----------|
| **P0** | Crash, data loss, or release blocker on a core flow | Move deletes source on failure; crash on launch; can't grant permission |
| **P1** | Core feature broken with no workaround; security/privacy violation | Copy silently no-ops; share leaks a `file://` URI; sort crashes on 10k dir |
| **P2** | Feature broken with a workaround, or incorrect non-blocking behavior | Wrong breadcrumb on a deep path; thumbnail occasionally blank |
| **P3** | Cosmetic / polish | Minor padding in landscape; string truncation |

**Exit gate (AC):** **0 open P0 and 0 open P1.** P2/P3 are logged with a triage
disposition (fix-now / fix-next / accept) signed off by PM; they do **not** block T7.6.
NFR-4 matrix passes when every required cell in §8 is **Pass** (or P2/P3-only).

---

## 4. Evidence & defect reporting

- **Per cell:** record Pass/Fail, build commit, device row, app version, and a one-line note.
- **Per defect:** file a child issue under **APP-86** (or the QA execution issue) titled
  `[Pn][<area>] <symptom>` with: device row, exact repro steps, expected vs actual,
  logcat excerpt (no PII/file-path leakage — NFR-8.1), and a screenshot/recording.
- **Crash/ANR:** attach full stack + `adb bugreport`. Any crash is **P0 until proven
  otherwise**.
- **Sign-off artifact:** QA posts a final results matrix (this doc's §8 grid filled in)
  as a comment/work product on the QA execution issue, plus the P0/P1 count.

---

## 5. Test data setup (per device)

Seed before each device run so charters are deterministic:

1. A **10,000-entry** directory (perf/NFR-1.2 + sort/scroll) — script: create via `adb shell`.
2. A nested tree ≥ 5 levels deep with mixed types (breadcrumb, search, copy/move).
3. Media set: ≥ 20 images, ≥ 5 videos, ≥ 5 audio, ≥ 5 docs, ≥ 3 APKs, ≥ 2 ZIPs
   (category hubs, thumbnails, filters, archives).
4. Files with edge-case names: spaces, unicode/RTL (`مرحبا.txt`), very long, dotfiles.
5. Where available: a removable SD/USB-OTG volume with a few files (FR-1.2, R5).
6. A large file > 500 MB (background copy/move WorkManager progress — FR-3.5).

---

## 6. Functional regression suites (charters)

Each charter: **Pre →** preconditions, **Steps →** actions, **Pass →** acceptance.

### A. Permissions & storage access (FR-1, NFR-4.2)
- **A1 First-run permission flow.** Pre: fresh install. Steps: launch → rationale →
  grant. Pass: correct permission requested **per API level** (broad on R1; SAF on R2;
  `READ_MEDIA_*` on R3+; partial-media path on R4/R7), home loads after grant.
- **A2 Denial & recovery.** Steps: deny, then re-trigger. Pass: graceful permission-denied
  state, no crash, re-request works.
- **A3 Partial media access (API 34+).** R4/R7. Steps: grant "selected photos". Pass:
  only selected media visible in hubs; "manage selection" reachable; no crash.
- **A4 Volume enumeration.** Pass: internal + removable SD + USB-OTG appear (FR-1.2);
  R5 with real removable media.

### B. File browser (FR-2)
- **B1 List/grid + metadata** (name, type icon, size, date). **B2 Navigate + tappable
  breadcrumb.** **B3 Sort** by name/size/date/type asc/desc (verify on the 10k dir, no
  crash, correct order). **B4 Show/hide hidden files** toggle. **B5 Pull-to-refresh**
  re-reads. Pass: each behaves per FR; **B6 rotation mid-scroll preserves scroll/state
  (NFR-4.3).**

### C. File operations (FR-3)
- **C1 Create folder** (+ invalid-name validation). **C2 Rename** file & folder.
  **C3 Copy** single & multi (incl. to SD/OTG on R5). **C4 Move** single & multi —
  **verify source removed only after copy succeeds; force a failure and confirm source
  intact (NFR-2.2 — P0 if violated).** **C5 Delete** single & multi with confirm;
  trash where supported (NFR-2.4). **C6 Conflict resolution** skip/replace/keep-both.
  **C7 Large/long op** runs in WorkManager with foreground progress; **survives
  process death and resumes (NFR-2.3)** — kill app mid-copy, confirm resume/report.

### D. Selection & batch (FR-4)
- **D1 Long-press multi-select**, select-all/clear-all, count. **D2 Batch bar** enables
  only valid actions (share disabled when a directory is selected).

### E. Search (FR-5)
- **E1 Name-substring search** of current tree (streaming, cancelable). **E2 Filters**:
  type/size/date range, AND-combined, removable chips. Pass: correct results, no ANR on
  large tree, cancel works.

### F. Media & categories (FR-6)
- **F1 Seven category hubs** with correct counts. **F2 Category detail grid** loads via
  `observeCategory`; live-updates on content change. **F3 Thumbnails** generated, cached,
  off-thread (no jank); scroll a large grid. **F4 Open/play** from category (intent).

### G. Archives (FR-7)
- **G1 Compress** selected → ZIP (progress, cancel cleans partial). **G2 Extract** ZIP to
  chosen folder; nested paths; conflict handling; **zip-slip path is rejected (security —
  P1 if it escapes).**

### H. Storage insights & favorites (FR-8, FR-9, FR-12)
- **H1 Per-volume breakdown by category** (FR-8.1). **H2 Largest files** top-N with
  delete/share (FR-8.2). **H3 Pin/unpin favorites** persisted across relaunch (Room,
  FR-9.1). **H4 Recents** list updates (FR-9.2). **H5 Home dashboard** aggregates
  volumes + categories + favorites + recents and each tile navigates correctly (FR-12).

### I. Settings & theming (FR-11)
- **I1 Theme** Light/Dark/System applies live and persists. **I2 Dynamic color** on
  API 31+ (R3/R4/R6/R7); graceful static theme on R1/R2 (API < 31). **I3 Toggles**:
  hidden files, default view (list/grid), default sort — persisted via DataStore and
  honored on next launch.

### J. Open / Share (FR-10, NFR-3.3)
- **J1 Open** file via chooser intent. **J2 Share** single & multiple via system sheet;
  **inspect the shared URI is a `content://` FileProvider URI with a scoped grant — a
  `file://` leak is P1.** R5 to real apps (Gmail/Drive).

### K. Release-build sanity (R8 / APP-85)
- Run **A1, B1–B3, C3–C4, C7, F1–F3, G1–G2, H3, I1** on the **`standardRelease`** build.
  Pass: no missing-class / reflection failures (WorkManager workers, Room, Hilt, Coil),
  identical behavior to debug. Any R8-only failure → keep-rule defect against APP-85.

### Accessibility & localization spot-checks (NFR-5, NFR-7 — owned by APP-82/APP-83, re-verified here)
- **L1 TalkBack** sweeps core flows (browse, ops, home); all interactive elements
  labeled, 48dp targets. **L2 Font scale 200%** — no clipping/overlap on core screens.
- **L3 RTL** locale (Arabic) — layout mirrors, no hardcoded strings, locale-aware
  date/size formatting.

---

## 7. Performance smoke (NFR-1, re-verify on matrix; deep profiling owned by APP-84)
- **P1 Cold start** to interactive home ≤ 1.5 s on a Pixel-6a-class row (R3/R4).
- **P2 10k-dir** first frame ≤ 300 ms warm / ≤ 800 ms cold; scroll ≥ 58 fps.
- **P3 LeakCanary** clean across full navigation on debug rows (NFR-9.2).

---

## 8. Results grid (QA fills in)

Legend: ✅ Pass · ❌ Fail (link defect) · ➖ N/A for this row · ⚠️ P2/P3 only.

| Suite | R1 26 ph | R2 30 ph | R3 33 ph | R4 35 ph | R5 phys | R6 33 tab | R7 35 tab |
|-------|:--:|:--:|:--:|:--:|:--:|:--:|:--:|
| A Permissions/storage |  |  |  |  |  |  |  |
| B Browser |  |  |  |  | ➖ |  |  |
| C File ops |  |  |  |  |  |  |  |
| D Selection/batch |  |  |  |  | ➖ |  |  |
| E Search |  |  |  |  | ➖ |  |  |
| F Media/categories |  |  |  |  |  |  |  |
| G Archives |  |  |  |  | ➖ |  |  |
| H Insights/favorites |  |  |  |  | ➖ |  |  |
| I Settings/theming |  |  |  |  | ➖ |  |  |
| J Open/Share |  |  |  |  |  |  |  |
| K Release-sanity |  |  |  |  | ➖ |  |  |
| L A11y/L10n |  | ➖ |  |  | ➖ |  |  |
| P Perf smoke | ➖ | ➖ |  |  | ➖ |  |  |
| **Config-change (rotate)** |  |  |  |  |  |  |  |

**Final:** P0 = ___ · P1 = ___ · P2 = ___ · P3 = ___ → **Gate: PASS only when P0 = 0 and P1 = 0.**

---

## 9. Done definition for T7.6
1. Every required cell in §8 is Pass (or P2/P3-only with PM triage sign-off).
2. P0 = 0 and P1 = 0 (open).
3. QA posts the filled §8 grid + defect links as the sign-off artifact.
4. Any defects found are filed as child issues; P0/P1 are fixed and re-verified before
   T7.6 closes, unblocking T7.7 (store assets) / T7.8 (staged rollout).
