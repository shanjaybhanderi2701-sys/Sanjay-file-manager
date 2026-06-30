# Package Structure — Filora

**Status:** Phase 1 — Draft for approval · **Last updated:** 2026-06-30
**Base package:** `com.appblish.filora`

---

## 1. Module layout (Gradle)

```
Filora/
├── app/                      # Application, MainActivity, NavHost, DI wiring
├── core/
│   ├── core-common/          # Result, dispatchers, extensions, formatters
│   ├── core-ui/              # Theme, Material3 tokens, shared composables
│   ├── core-domain/          # Entities, repository interfaces, use cases (pure Kotlin)
│   ├── core-data/            # Repo impls, data sources, mappers
│   └── core-database/        # Room entities, DAOs, database
└── feature/
    ├── feature-home/         # Home dashboard
    ├── feature-browser/      # Directory browser + file operations UI
    ├── feature-search/       # Search & filters
    ├── feature-media/        # Category hubs (images/video/audio/docs/…)
    ├── feature-storage/      # Storage insights / largest files
    └── feature-settings/     # Settings & theming
```

Feature modules depend on `core-*`; they do **not** depend on each other.
`app` depends on all; `core-domain` depends on nothing Android.

## 2. Source package tree (within modules)

```
com.appblish.filora
├── app
│   ├── FiloraApplication.kt
│   ├── MainActivity.kt
│   └── navigation/            # NavHost, route graph, NavRoute sealed types
├── core
│   ├── common/
│   │   ├── result/            # Result<T>, OperationError
│   │   ├── dispatcher/        # @IoDispatcher, @DefaultDispatcher qualifiers
│   │   └── util/              # size/date formatters, file extensions
│   ├── ui/
│   │   ├── theme/             # Color, Type, Shape, FiloraTheme
│   │   └── component/         # FileRow, GridTile, EmptyState, ProgressDialog
│   ├── domain/
│   │   ├── model/             # FileItem, StorageVolume, MediaCategory, SortOrder
│   │   ├── repository/        # FileRepository, StorageRepository, MediaRepository,
│   │   │                      #   FavoritesRepository, SettingsRepository, ArchiveRepository
│   │   └── usecase/           # ListDirectoryUseCase, CopyFilesUseCase, MoveFilesUseCase,
│   │                          #   DeleteFilesUseCase, RenameUseCase, CreateFolderUseCase,
│   │                          #   SearchFilesUseCase, CompressUseCase, ExtractUseCase, …
│   ├── data/
│   │   ├── repository/        # *RepositoryImpl
│   │   ├── source/
│   │   │   ├── file/          # FileSystemDataSource (java.io)
│   │   │   ├── saf/           # SafDataSource (DocumentFile)
│   │   │   ├── media/         # MediaStoreDataSource
│   │   │   └── prefs/         # DataStore source
│   │   ├── mapper/            # cursor/DocumentFile ↔ domain
│   │   ├── worker/            # CopyWorker, MoveWorker, ZipWorker, ExtractWorker
│   │   └── di/                # RepositoryModule, DataStoreModule, WorkManagerModule
│   └── database/
│       ├── FiloraDatabase.kt
│       ├── entity/            # FavoriteEntity, RecentEntity, ThumbnailIndexEntity
│       ├── dao/               # FavoriteDao, RecentDao
│       └── di/                # DatabaseModule
└── feature
    ├── home/        { HomeScreen, HomeViewModel, HomeUiState }
    ├── browser/     { BrowserScreen, BrowserViewModel, BrowserUiState, selection/ }
    ├── search/      { SearchScreen, SearchViewModel, SearchUiState, filter/ }
    ├── media/       { MediaCategoryScreen, MediaViewModel, MediaUiState }
    ├── storage/     { StorageScreen, StorageViewModel, LargestFilesScreen }
    └── settings/    { SettingsScreen, SettingsViewModel }
```

## 3. Naming conventions

- **UseCases:** `VerbNounUseCase`, single public `operator fun invoke(...)`.
- **ViewModels:** `XScreenViewModel` exposing `StateFlow<XUiState>` + event fns.
- **UI state:** immutable `data class XUiState`, default = loading/empty.
- **Repos:** interface in `core-domain/repository`, impl `XRepositoryImpl` in `core-data`.
- **DI modules:** suffix `Module`, installed in the narrowest sensible component.
- **Tests:** mirror package under `src/test` / `src/androidTest`, suffix `Test`.

## 4. Module dependency rules (enforced)

- `core-domain` → (nothing Android).
- `core-data` → `core-domain`, `core-database`, `core-common`.
- `feature-*` → `core-domain`, `core-ui`, `core-common` (never another `feature-*`).
- `app` → everything; owns navigation and DI graph assembly.
