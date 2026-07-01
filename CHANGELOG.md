# Changelog

All notable changes to Filora are recorded here. This file leads with user
impact; engineering detail lives in the linked milestone issues (APP-###) and in
[`docs/phase-1/`](docs/phase-1/).

The format is based on [Keep a Changelog](https://keepachangelog.com/); Filora
follows semantic versioning once it reaches the Play Store.

## [Unreleased] — Phase 1 (v0.1.0)

**Audience:** early testers and reviewers evaluating the first end-to-end build.

Phase 1 delivers a complete, private, Play-default file manager: browse your
files, find them fast, work with your media and storage, and keep the tools you
use most one tap away — all with least-privilege permissions.

### Added — what you can do

- **Home dashboard.** Open to an overview of your storage volumes, media
  category counts, recent files, and pinned favorites, each tapping through to
  the right screen. *(M6)*
- **File browsing.** Navigate folders in list or grid, sort, show/hide hidden
  files, and pull to refresh. *(M3)*
- **File operations.** Select multiple items and create, rename, delete,
  copy/move (with skip / replace / keep-both conflict handling), and share.
  Long-running copies and moves run in the background with progress. *(M3, M6)*
- **Search.** Search your files by name as you type, with removable filters for
  type, size, and date; results are cancelable and stream in, and tapping one
  jumps straight to its location. *(M5)*
- **Media hubs.** Browse seven media categories (images, video, audio, and more)
  with fast thumbnails, open a category grid, and play or share items. *(M4)*
- **Storage insight.** See per-volume used/free space and a by-category
  breakdown, plus a largest-files view to reclaim space with open / share /
  delete. *(M6)*
- **Archives.** Compress files and folders to ZIP and extract them, safely
  (protected against path-traversal), as background jobs with progress. *(M5)*
- **Favorites.** Pin any file or folder from Browse or Media and reach it from
  the Home strip. *(M8)*
- **Settings & theming.** Choose your theme and dynamic color, default list/grid
  layout, sort order, and hidden-file preference; changes apply immediately and
  persist across launches. *(M7)*
- **Localization & RTL.** Fully externalized strings with English and Arabic, and
  right-to-left layout support. *(M7, NFR-7)*
- **Accessibility.** 48dp touch targets, button roles and click labels on
  interactive tiles, and live-region progress announcements. *(M7, NFR-5)*

### Privacy & permissions

- Ships as the **least-privilege "standard" flavor** — no all-files access. The
  `MANAGE_EXTERNAL_STORAGE` opt-in exists only in a separate `fullaccess` build
  and is never the Play default.
- **No data collected or shared.** See
  [`docs/phase-1/release-assets/privacy-policy.md`](docs/phase-1/release-assets/privacy-policy.md)
  and the [data-safety form](docs/phase-1/release-assets/data-safety-form.md).

### Performance & size

- R8 full-mode shrinking with a **CI-enforced 12 MB release budget**, baseline
  profiles for faster cold start, and LeakCanary leak detection in debug builds.
  *(M7)*

### Under the hood (for contributors)

- **M0** — multi-module foundation: convention plugins, version catalog, base
  theme, CI gates (ktlint / detekt / module rules / lint / tests).
- **M1** — data foundations: `StorageRepository` (volume enumeration), SAF tree
  picker with persistable grants, `MediaStore` read source.
- **M2** — runtime permission flow with settings deep-link and all-files opt-in.
- **M3** — file browser + full file-operation stack (create/rename/delete/
  copy/move/share) and WorkManager foreground workers.
- **M4** — media category hubs, Coil thumbnails + bounded cache, category detail,
  open/play/share intents, permission-aware Home counts.
- **M5** — streaming search + filter chips, ZIP compress/extract workers.
- **M6** — Home dashboard assembly, storage breakdown, largest-files view,
  favorites/recents, Room schema v1 + migration test.
- **M7** — settings/theming (DataStore), localization + RTL, accessibility pass,
  R8/release config, perf/memory hardening, store assets.
- **M8** — favorites pin/unpin entry points.

### Known limitations

- Physical-device QA is deferred to v1.1; the Phase-1 ship gate runs the
  automated suite on the CI emulator matrix (see
  [`qa-regression-plan.md`](docs/phase-1/qa-regression-plan.md)).
- Store graphics production and final signing keystore provisioning are release-
  time tasks tracked separately.
