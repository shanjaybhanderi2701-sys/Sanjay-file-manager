# Filora — As-Built Architecture (Phase 1)

This document describes how the Filora codebase is **actually structured** after
milestones M0–M8. For the original planning intent see
[`04-technical-architecture.md`](04-technical-architecture.md) and
[`07-module-breakdown.md`](07-module-breakdown.md); where the two disagree, this
document reflects the shipped code.

- **Platform:** Android, `minSdk 24` / `targetSdk 35` / `compileSdk 35`.
- **Language / build:** Kotlin `2.1.0`, AGP `8.7.3`, Gradle wrapper `8.11.1`, JDK 17.
- **UI:** Jetpack Compose (BOM `2024.12.01`), Material 3 with dynamic color.
- **DI:** Hilt `2.54`. **Persistence:** Room `2.6.1`, DataStore `1.1.1`.
- **Background work:** WorkManager `2.10.0`. **Images:** Coil `2.7.0`.

---

## 1. Module topology

Filora is a multi-module Gradle project. Build configuration lives **only** in the
convention plugins under [`build-logic/`](../../build-logic); modules apply a
plugin rather than repeating Android/Kotlin config.

```
app                         Application, MainActivity, NavHost, Hilt graph, flavors
build-logic                 Convention plugins (single source of build config)
baselineprofile             Macrobenchmark module producing the R8 baseline profile
core/
  core-common               Result, OperationError, dispatchers, formatters   (pure JVM)
  core-domain               Entities + repository interfaces + use cases       (pure JVM)
  core-ui                   FiloraTheme (M3 + dynamic color), shared composables, Coil loader
  core-database             Room database, entities, DAOs, DI
  core-data                 Repository impls, data sources, WorkManager operations, DI
feature/
  feature-home  feature-browser  feature-search
  feature-media feature-storage  feature-settings
```

### Dependency edges (CI-enforced by `checkModuleDependencies`)

```
core-common  ◄── core-domain, core-ui, core-database
core-common, core-domain, core-database  ◄── core-data
core-common, core-domain, core-ui        ◄── every feature-*   (via filora.android.feature)
everything                               ◄── app
```

Rules the build enforces:

- **`feature-*` modules never depend on each other.** Cross-feature navigation is
  resolved in `app` through typed routes, not module dependencies.
- **`core-common` and `core-domain` are pure Kotlin** — no `android.*`, no Hilt,
  no Compose. They compile with `compileKotlin` (JVM), not the Android tasks.
- **Features depend on `core-domain` interfaces only.** They never see
  `core-data` or `core-database`; the implementations are bound into the Hilt
  graph assembled in `app`, so a feature is compiled against seams, not impls.

---

## 2. Layering (data → domain → feature)

Filora follows Clean Architecture + MVVM. Data flows one direction; exceptions
never cross a layer boundary (`core-common`'s `Result<T>` / `OperationError`
carry failures instead).

```
┌─────────────────────────────────────────────────────────────┐
│ feature-* (Compose UI + ViewModel)                          │
│   Screen (stateless) ── observes ──► StateFlow<XUiState>    │
│   ViewModel ── calls ──► UseCase                            │
└───────────────────────────────┬─────────────────────────────┘
                                 │ depends on (interfaces only)
┌───────────────────────────────▼─────────────────────────────┐
│ core-domain (pure JVM)                                       │
│   Entities (FileItem, MediaItem, StorageVolume, …)          │
│   Repository interfaces (the seams — see §3)                │
│   UseCases (VerbNounUseCase, return Result<T> / Flow<T>)    │
└───────────────────────────────┬─────────────────────────────┘
                                 │ implemented by (bound in app's Hilt graph)
┌───────────────────────────────▼─────────────────────────────┐
│ core-data / core-database                                    │
│   RepositoryImpl + data sources (java.io, SAF, MediaStore)  │
│   Room DAOs/entities · DataStore · WorkManager operations   │
└─────────────────────────────────────────────────────────────┘
```

- **UI state is immutable.** ViewModels expose `StateFlow<XUiState>` and receive
  events; Screens are stateless and hoist state.
- **Dispatchers are injected** via `@IoDispatcher` / `@DefaultDispatcher` — no
  hard-coded `Dispatchers.*` in domain/data classes.
- **Strings are externalized** (`stringResource`) and error text is carried as
  `@StringRes` (`errorMessageRes`), never as raw English — see
  [`localization-rtl.md`](localization-rtl.md).

---

## 3. Key seams

These interfaces are the contracts the whole app is built against. UI and use
cases depend on the interface; `core-data` provides the implementation.

| Seam | Defined in | Implementation | Responsibility |
|---|---|---|---|
| `FileRepository` | `core-domain` | `FileRepositoryImpl` (+ `FileSystem` / `Saf` data sources) | List, create, rename, delete, copy/move primitives over `java.io` and SAF |
| `MediaRepository` | `core-domain` | `MediaRepositoryImpl` (`MediaStoreSource`, `MediaClassifier`) | Category counts + `observeCategory` backed by a `ContentObserver`; `SecurityException → PermissionDenied` |
| `StorageRepository` | `core-domain` | volume enumeration + `categorySizes` + `largestFiles` | Per-volume used/free and by-category breakdown |
| `SearchRepository` | `core-domain` | `FileSystemSearchRepository` (`FileTreeWalker` / `DocumentTreeWalker`) | Cold streaming `Flow` search with `SearchProgress` and filters |
| `FavoritesRepository` | `core-domain` | `FavoritesRepositoryImpl` (Room `FavoriteDao`) | Pin/unpin + observe pinned items |
| `SettingsRepository` | `core-domain` | `SettingsRepositoryImpl` (DataStore) | `UserPreferences` (theme, dynamic color, layout, sort, hidden) |
| `OperationScheduler` | `core-data` | WorkManager wrapper | Enqueue copy/move/delete/compress/extract as foreground jobs; expose progress `Flow`s |

**File operations** (copy/move/delete, ZIP compress/extract) are long-running, so
they run as `CoroutineWorker`s scheduled through `OperationScheduler`. The domain
`UseCase`s hold the logic (conflict resolution, zip-slip safety); the workers
delegate to them and publish foreground progress notifications.

---

## 4. Cross-cutting concerns

- **Navigation.** Typed `Route` sealed hierarchy + `kotlinx.serialization`;
  `FiloraNavHost` owns the graph and a bottom bar with `TopLevelDestination`s.
  Cross-feature jumps go through pure mapping helpers (e.g. `homeItemRoute`).
  See [`06-navigation-flow.md`](06-navigation-flow.md).
- **Permissions.** Runtime media/storage permissions are permission-aware at the
  UI layer; the `MANAGE_EXTERNAL_STORAGE` opt-in is gated behind
  `BuildConfig.FULL_ACCESS_SUPPORTED` and only exists in the `fullaccess` flavor.
  See [`02-functional-requirements.md`](02-functional-requirements.md) §Permissions.
- **Theming & accessibility.** `FiloraTheme` (M3 + dynamic color) is driven live
  from Settings. Core-flow tiles/rows use `clickableTile` (`Role.Button`, 48dp
  min target, `onClickLabel`) and progress uses a `liveRegion` — see the
  accessibility audit under `docs/phase-1/`.
- **Performance / release.** LeakCanary (debug only), ProfileInstaller +
  `:baselineprofile` for faster cold start, R8 full-mode with resource shrinking,
  a non-debuggable release, and a CI 12 MB size gate
  (`verifyStandardReleaseSizeBudget`). See
  [`11-performance-memory-hardening.md`](11-performance-memory-hardening.md).

---

## 5. Testing seams

- **Pure-JVM modules** (`core-common`, `core-domain`) unit-test with JUnit + Truth
  + Turbine + MockK and no Android/Robolectric.
- **Fakes over mocks for repositories:** use cases and ViewModels are tested
  against hand-written fake repositories implementing the `core-domain` seams.
- **Instrumented tests** (Compose UI, Room migration, navigation, the M4 Hilt
  smoke test) run on the CI emulator matrix. See
  [`qa-regression-plan.md`](qa-regression-plan.md).
