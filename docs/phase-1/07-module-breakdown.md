# Module Breakdown — Filora

**Status:** Phase 1 — Draft for approval · **Last updated:** 2026-06-30

Each module below lists its responsibility, key components, dependencies, and the
requirements (`FR-*`) it satisfies.

---

## core-common
- **Responsibility:** shared kernel — `Result<T>`, `OperationError`, dispatcher
  qualifiers, size/date/path formatters, Kotlin extensions.
- **Depends on:** nothing.
- **Public surface:** `Result`, `OperationError`, `@IoDispatcher`, formatters.

## core-ui
- **Responsibility:** Material 3 theme (Filora palette + dynamic color), typography,
  shapes, and reusable composables (`FileRow`, `GridTile`, `EmptyState`,
  `ConfirmDialog`, `ProgressBarRow`, breadcrumb bar).
- **Depends on:** core-common.
- **Satisfies:** FR-11 theming, shared UI for all features.

## core-domain  *(pure Kotlin)*
- **Responsibility:** entities, repository interfaces, and all use cases.
- **Entities:** `FileItem`, `StorageVolume`, `MediaCategory`, `SortOrder`, `SearchQuery`.
- **Repositories (interfaces):** `FileRepository`, `StorageRepository`,
  `MediaRepository`, `ArchiveRepository`, `FavoritesRepository`, `SettingsRepository`.
- **Use cases:** list/sort, create/rename/delete, copy/move, search, compress/extract,
  storage breakdown, favorites/recents, settings.
- **Depends on:** core-common only.
- **Satisfies:** business rules behind FR-2…FR-9.

## core-data
- **Responsibility:** implement domain repositories over Android sources.
- **Sources:** `FileSystemDataSource` (java.io), `SafDataSource` (DocumentFile),
  `MediaStoreDataSource`, `PreferencesDataSource` (DataStore).
- **Workers:** `CopyWorker`, `MoveWorker`, `DeleteWorker`, `ZipWorker`, `ExtractWorker`.
- **DI:** `RepositoryModule`, `DataStoreModule`, `WorkManagerModule`.
- **Depends on:** core-domain, core-database, core-common.
- **Satisfies:** FR-1, FR-3, FR-5, FR-6, FR-7, FR-8 data paths.

## core-database
- **Responsibility:** Room database, entities, DAOs, migrations.
- **Entities:** `FavoriteEntity`, `RecentEntity`, `ThumbnailIndexEntity`.
- **Depends on:** core-common.
- **Satisfies:** FR-9 persistence; thumbnail/index caching.

## feature-home
- **Responsibility:** dashboard aggregating volumes, category shortcuts, favorites, recents.
- **Components:** `HomeScreen`, `HomeViewModel`, `HomeUiState`.
- **Satisfies:** FR-12, entry points to FR-6/FR-8/FR-9.

## feature-browser
- **Responsibility:** directory listing, navigation, selection, and file operations UI.
- **Components:** `BrowserScreen`, `BrowserViewModel`, selection state, action sheet,
  rename/new-folder/conflict dialogs, progress surface.
- **Satisfies:** FR-2, FR-3, FR-4, FR-10.

## feature-search
- **Responsibility:** query input, filter chips, streamed results.
- **Components:** `SearchScreen`, `SearchViewModel`, filter models.
- **Satisfies:** FR-5.

## feature-media
- **Responsibility:** category hubs with thumbnails backed by MediaStore.
- **Components:** `MediaCategoryScreen`, `MediaViewModel`.
- **Satisfies:** FR-6.

## feature-storage
- **Responsibility:** per-volume breakdown and largest-files view.
- **Components:** `StorageScreen`, `LargestFilesScreen`, `StorageViewModel`.
- **Satisfies:** FR-8.

## feature-settings
- **Responsibility:** theme, default view/sort, hidden-files toggle, about.
- **Components:** `SettingsScreen`, `SettingsViewModel`, `AboutScreen`.
- **Satisfies:** FR-11.

## app
- **Responsibility:** `FiloraApplication` (Hilt), `MainActivity`, `NavHost`, route graph,
  permission/onboarding gating, DI assembly, baseline profiles.
- **Depends on:** all modules.
- **Satisfies:** FR-1 permission flow, navigation across all features.

---

## Build order (by dependency)

1. core-common → 2. core-ui / core-domain / core-database → 3. core-data →
4. app shell + navigation → 5. feature-home → 6. feature-browser →
7. feature-media / feature-search → 8. feature-storage / feature-settings.
