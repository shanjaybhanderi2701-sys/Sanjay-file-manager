# Project Plan & Engineering Task Breakdown — Filora File Manager

**Owner:** appblish · CTO · **Status:** Phase 1 — **Awaiting approval**
**Last updated:** 2026-06-30

This is the master plan. It links the Phase 1 document set and defines the
**milestones** and **small engineering tasks** (each with acceptance criteria and
dependencies) that engineering will execute **after approval**. No application code
is written in Phase 1.

## Phase 1 document set
1. [Product Requirements Document](01-prd.md)
2. [Functional Requirements](02-functional-requirements.md)
3. [Non-functional Requirements](03-non-functional-requirements.md)
4. [Technical Architecture](04-technical-architecture.md)
5. [Package Structure](05-package-structure.md)
6. [Navigation Flow](06-navigation-flow.md)
7. [Module Breakdown](07-module-breakdown.md)
8. [Development Roadmap](08-development-roadmap.md)
9. [Risk Assessment](09-risk-assessment.md)
10. [Release Plan](10-release-plan.md)

## Stack (fixed by mandate)
Native Android · Kotlin · Jetpack Compose · Material 3 · MVVM · Clean Architecture ·
Hilt · Coroutines · Flow · Room · WorkManager · Navigation Compose · MediaStore ·
Storage Access Framework · DocumentFile.

---

## Execution model
- One **milestone = one parent issue** (epic) assigned to the Founding Android Engineer.
- Each **task below = one child issue** with the stated acceptance criteria and
  dependency links (`blockedByIssueIds`).
- These issues are created **only after this plan is approved** (per the wait-for-approval mandate).
- Tasks are intentionally small (≈0.5–2 days each) and independently reviewable.

Legend: **AC** = acceptance criteria · **Dep** = depends on.

---

## M0 — Foundation & Scaffolding
- **T0.1 Gradle multi-module + version catalog.** AC: modules from Package Structure
  exist and build; `libs.versions.toml` defines all deps; `./gradlew build` green. Dep: —
- **T0.2 Hilt setup.** AC: `FiloraApplication` + Hilt graph compiles; a sample
  `@HiltViewModel` injects. Dep: T0.1
- **T0.3 Material 3 theme (core-ui).** AC: FiloraTheme with light/dark + dynamic color
  (API 31+); typography/shape tokens. Dep: T0.1
- **T0.4 Navigation shell.** AC: single-activity NavHost with `home` start destination
  (empty screen). Dep: T0.2, T0.3
- **T0.5 Domain kernel.** AC: `Result`, `OperationError`, dispatcher qualifiers,
  base entities (`FileItem`, `StorageVolume`) defined with unit tests. Dep: T0.1
- **T0.6 CI pipeline.** AC: CI runs build + ktlint + detekt + unit tests on PR; badge green. Dep: T0.1

## M1 — Storage & Permissions  *(Dep: M0)*
- **T1.1 Permission rationale + request flow.** AC: FR-1.1 — rationale screen; grant
  → home; deny → SAF fallback path. Dep: T0.4
- **T1.2 StorageRepository + volume enumeration.** AC: FR-1.2 — list internal/SD/USB
  volumes with used/total. Dep: T0.5
- **T1.3 SAF tree picker integration.** AC: user can grant a tree URI; persisted
  permission survives restart. Dep: T1.1
- **T1.4 MediaStore data source (read).** AC: query media counts per category. Dep: T0.5

## M2 — File Browser  *(Dep: M1)*
- **T2.1 ListDirectoryUseCase + FileRepository (file/SAF).** AC: FR-2.1 lists entries
  with metadata, off-main, streamed via Flow. Dep: T1.2, T1.3
- **T2.2 Browser screen list/grid + view toggle.** AC: FR-2.1 toggle persists. Dep: T2.1, T0.3
- **T2.3 Folder navigation + breadcrumb.** AC: FR-2.2 push per level; breadcrumb jumps. Dep: T2.2
- **T2.4 Sorting.** AC: FR-2.3 name/size/date/type asc/desc, persisted. Dep: T2.2
- **T2.5 Hidden-files toggle + pull-to-refresh.** AC: FR-2.4, FR-2.5. Dep: T2.2
- **T2.6 Large-directory performance.** AC: NFR-1.2/1.3 met on 10k-entry dir. Dep: T2.2

## M3 — File Operations  *(Dep: M2)*
- **T3.1 Create folder + rename use cases & dialogs.** AC: FR-3.1, FR-3.2. Dep: T2.1
- **T3.2 Multi-select + batch action bar.** AC: FR-4.1 count + enabled actions. Dep: T2.2
- **T3.3 Delete (single/batch) + confirm + trash.** AC: FR-3.4, NFR-2.2. Dep: T3.2
- **T3.4 Copy/Move use cases + conflict resolver.** AC: FR-3.3 skip/replace/keep-both;
  move = copy-verify-delete. Dep: T3.2
- **T3.5 WorkManager workers + foreground progress.** AC: FR-3.5, NFR-2.3 survive
  backgrounding/process death. Dep: T3.4
- **T3.6 Open/share via intents + FileProvider.** AC: FR-10.1, FR-10.2. Dep: T2.2

## M4 — Media & Categories  *(Dep: M3)*
- **T4.1 Category hubs UI.** AC: FR-6.1 Images/Video/Audio/Docs/Downloads/APK/Archives
  with counts. Dep: T1.4
- **T4.2 Thumbnail loading + bounded cache.** AC: FR-6.2, NFR-9.2 no scroll block. Dep: T4.1
- **T4.3 Open/play from category via intent.** AC: FR-6.1 tap opens correct app. Dep: T4.1, T3.6

## M5 — Search & Archives  *(Dep: M3)*
- **T5.1 SearchFilesUseCase (streaming, cancelable).** AC: FR-5.1. Dep: T2.1
- **T5.2 Search screen + filter chips.** AC: FR-5.2 type/size/date filters combine. Dep: T5.1
- **T5.3 ZIP compress worker.** AC: FR-7.1 progress, chosen location. Dep: T3.5
- **T5.4 ZIP extract worker.** AC: FR-7.2 nested paths, conflict handling. Dep: T3.5

## M6 — Storage Insights & Favorites  *(Dep: M2, M3, M4)*
- **T6.1 Room: favorites/recents entities + DAOs.** AC: schema + migration tests. Dep: T0.1
- **T6.2 Favorites/Recents use cases + home wiring.** AC: FR-9.1, FR-9.2 persist. Dep: T6.1
- **T6.3 Storage breakdown screen.** AC: FR-8.1 per-volume by category. Dep: T1.2, T1.4
- **T6.4 Largest files view.** AC: FR-8.2 top-N, delete/share. Dep: T6.3
- **T6.5 Home dashboard assembly.** AC: FR-12 volumes+categories+favorites+recents. Dep: T6.2, T6.3

## M7 — Hardening, Polish, Release  *(Dep: all)*
- **T7.1 Settings + theming screen.** AC: FR-11.1, FR-11.2 persisted via DataStore. Dep: T0.3
- **T7.2 Accessibility pass.** AC: NFR-5 TalkBack on core flows, contrast, 48dp. Dep: features done
- **T7.3 Localization scaffolding + RTL.** AC: NFR-7 no hardcoded strings. Dep: features done
- **T7.4 Performance + memory hardening.** AC: NFR-1, NFR-9 (LeakCanary clean, baseline profiles). Dep: features done
- **T7.5 R8/minify + release config.** AC: NFR-9.1 size budget; non-debuggable release. Dep: T7.4
- **T7.6 QA regression on device matrix.** AC: 0 P0/P1; NFR-4 matrix pass. Dep: T7.2–T7.5
- **T7.7 Store assets + data-safety + privacy policy.** AC: Release Plan §6 complete. Dep: T7.6
- **T7.8 GA staged rollout setup.** AC: Release Plan §10 checklist complete. Dep: T7.7

---

## Totals
- **8 milestones (epics)**, **39 engineering tasks**, sequenced by dependency.
- Estimated **~10–11 weeks** to GA for 1–2 engineers (see Roadmap timeline).

## What happens on approval
1. Create 8 milestone parent issues (assigned: Founding Android Engineer).
2. Create the 39 task child issues with the ACs and `blockedByIssueIds` above.
3. Engineer begins **T0.1** (foundation). No code is written before this approval.

## Decisions requested from the board
- Approve this plan as-is, or comment with changes (scope, min API, brand name,
  diagnostics opt-in) and I will revise before creating issues.
