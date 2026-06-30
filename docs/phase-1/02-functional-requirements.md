# Functional Requirements — Filora

**Status:** Phase 1 — Draft for approval · **Last updated:** 2026-06-30

Each requirement has an ID (`FR-x`), a statement, and acceptance criteria (AC).
IDs are referenced by the engineering task breakdown in the Project Plan.

---

## FR-1 Storage & Permissions
- **FR-1.1** App requests storage access appropriate to API level (READ_MEDIA_* on
  API 33+, SAF for tree access; `MANAGE_EXTERNAL_STORAGE` only if justified and
  gated behind an explicit rationale screen).
  - *AC:* On first launch a permission rationale screen explains why access is
    needed; granting proceeds to home; denying still allows SAF-scoped browsing.
- **FR-1.2** Access internal storage, removable SD, and USB-OTG volumes via
  `StorageManager`/SAF tree URIs.
  - *AC:* All mounted volumes appear in the storage list with used/total size.

## FR-2 File Browser
- **FR-2.1** Browse a directory as a list or grid; show name, type icon, size, modified date.
  - *AC:* Toggling list/grid persists per user; folders sort before files by default.
- **FR-2.2** Navigate into folders and back; breadcrumb path is tappable.
  - *AC:* Tapping any breadcrumb segment jumps to that ancestor.
- **FR-2.3** Sort by name / size / date / type, ascending or descending.
  - *AC:* Selected sort persists across sessions per directory-scope setting.
- **FR-2.4** Show/hide hidden (dot) files via a toggle.
  - *AC:* Default hidden; toggle reveals and persists.
- **FR-2.5** Pull-to-refresh re-reads the directory.
  - *AC:* External changes (new file added by another app) appear after refresh.

## FR-3 File Operations
- **FR-3.1** Create new folder.
  - *AC:* Duplicate name is rejected with inline error.
- **FR-3.2** Rename file/folder.
  - *AC:* Invalid characters rejected; rename reflected without full reload.
- **FR-3.3** Copy and Move (single & multi).
  - *AC:* Conflicts prompt skip/replace/keep-both; progress shown; cancelable.
- **FR-3.4** Delete (single & multi) with confirm; move-to-trash where supported.
  - *AC:* Confirmation required; deleted items removed from list on success.
- **FR-3.5** Long, large operations run in background (WorkManager) with a
  foreground notification and progress.
  - *AC:* App can be backgrounded mid-copy; operation completes; notification updates.

## FR-4 Selection & Batch
- **FR-4.1** Multi-select via long-press; select-all / clear-all.
  - *AC:* Action bar shows count and enabled batch actions (move/copy/delete/share/zip).

## FR-5 Search
- **FR-5.1** Search current tree by name substring.
  - *AC:* Results stream in as found; cancelable; empty-state shown for no match.
- **FR-5.2** Filter by type (image/video/audio/doc/archive/apk), size range, date range.
  - *AC:* Filters combine (AND); active filters shown as removable chips.

## FR-6 Media & Categories
- **FR-6.1** Category hubs backed by MediaStore: Images, Video, Audio, Documents,
  Downloads, APKs, Archives.
  - *AC:* Each hub lists items with thumbnails (image/video) and counts; tap opens/plays via intent.
- **FR-6.2** Image/video thumbnails generated and cached.
  - *AC:* Thumbnails appear without blocking scroll; cache survives navigation.

## FR-7 Archives
- **FR-7.1** Compress selected items into a ZIP.
  - *AC:* Output archive created at chosen location; progress shown.
- **FR-7.2** Extract a ZIP to a chosen folder.
  - *AC:* Extraction handles nested paths; conflicts prompt as in FR-3.3.

## FR-8 Storage Insights
- **FR-8.1** Per-volume storage breakdown by category.
  - *AC:* Used/free shown; tapping a category opens its hub filtered to that volume.
- **FR-8.2** Largest files view (top N by size).
  - *AC:* Sorted desc; supports delete/share from the list.

## FR-9 Favorites & Recents
- **FR-9.1** Pin folders/files as favorites (stored in Room).
  - *AC:* Favorites appear on home; unpin removes them.
- **FR-9.2** Recent files/locations list.
  - *AC:* Most-recent first; clearable.

## FR-10 Open / Share
- **FR-10.1** Open a file with the default/chooser app via intent.
  - *AC:* Unknown types still offer the chooser; no crash on no-handler.
- **FR-10.2** Share single/multiple files via system share sheet (FileProvider URIs).
  - *AC:* Receiving apps can read the shared content.

## FR-11 Settings & Theming
- **FR-11.1** Light/Dark/System theme; Material 3 dynamic color (API 31+).
  - *AC:* Theme change applies immediately and persists.
- **FR-11.2** Toggle hidden files, default view (list/grid), default sort.
  - *AC:* Settings persisted via DataStore and respected app-wide.

## FR-12 Home Dashboard
- **FR-12.1** Home aggregates: storage volumes, category shortcuts, favorites, recents.
  - *AC:* Home is the start destination and reflects live storage state on resume.
