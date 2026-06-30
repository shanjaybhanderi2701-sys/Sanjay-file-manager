# Development Roadmap — Filora

**Status:** Phase 1 — Draft for approval · **Last updated:** 2026-06-30

Milestone-based plan. Durations are engineering estimates for a small team
(1–2 Android engineers) and will be confirmed at kickoff. The detailed, sequenced
engineering tasks (with acceptance criteria & dependencies) live in the
**Project Plan** document; this roadmap is the milestone-level view.

---

## Milestone M0 — Foundation & Scaffolding  *(~1 week)*
Project skeleton, modules, DI, theme, CI, navigation shell.
- Gradle multi-module + version catalog; Hilt wired; Material 3 theme; empty NavHost;
  CI (build + lint + unit tests); base entities and Result type.
- **Exit:** app installs and shows an empty themed Home; CI green.

## Milestone M1 — Storage & Permissions  *(~1 week)*
Permission flow and volume enumeration; storage repository over java.io/SAF/MediaStore.
- Permission rationale screen; SAF tree picker; list volumes with sizes.
- **Exit:** user can grant access and see all storage volumes.

## Milestone M2 — File Browser  *(~1.5 weeks)*
Browse, sort, view toggle, breadcrumb, hidden-files, refresh.
- `ListDirectoryUseCase` + Browser screen (list/grid), sorting, navigation.
- **Exit:** smooth browsing of a 10k-entry directory meeting NFR-1.

## Milestone M3 — File Operations  *(~2 weeks)*
Create/rename/delete/copy/move, multi-select, conflict handling, WorkManager.
- Operation use cases + workers + progress + batch action bar.
- **Exit:** all operations work single & batch, survive backgrounding, no data loss.

## Milestone M4 — Media & Categories  *(~1.5 weeks)*
MediaStore-backed hubs with thumbnails; open/share via intents.
- Category hubs, thumbnail cache, FileProvider sharing.
- **Exit:** all categories populate with thumbnails; open/share works.

## Milestone M5 — Search & Archives  *(~1.5 weeks)*
Tree search with filters; ZIP compress/extract.
- `SearchFilesUseCase` (streaming), filter chips; `Zip/ExtractWorker`.
- **Exit:** search returns filtered results; zip/unzip work with progress.

## Milestone M6 — Storage Insights & Favorites  *(~1 week)*
Per-volume breakdown, largest files, favorites/recents (Room).
- Storage screens; Room favorites/recents; home wiring.
- **Exit:** insights accurate; favorites/recents persist.

## Milestone M7 — Hardening, Polish, Release  *(~1.5 weeks)*
Accessibility, localization scaffolding, perf passes, R8, baseline profiles, QA,
store assets, beta → GA.
- **Exit:** meets all NFRs; release AAB signed; GA checklist complete.

---

## Timeline (indicative)

```
Week:  1   2   3   4   5   6   7   8   9  10  11
M0    ██
M1        ██
M2            ███
M3                ████
M4                      ███
M5                          ███
M6                              ██
M7                                  ███
```

≈ **10–11 weeks** to GA for a 1–2 engineer team, sequential with light overlap.
Parallelization (e.g. M4 media alongside M3 polish) can compress this.

## Dependencies between milestones
- M1 requires M0. M2 requires M1. M3 requires M2.
- M4, M5 require M3 (operations + browser). M6 requires M2/M3/M4.
- M7 requires all prior milestones feature-complete.
