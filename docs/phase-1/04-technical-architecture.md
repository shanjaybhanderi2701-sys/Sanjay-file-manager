# Technical Architecture — Filora

**Status:** Phase 1 — Draft for approval · **Last updated:** 2026-06-30

---

## 1. Architectural Style

Filora uses **Clean Architecture** with **MVVM** at the presentation layer.
Three concentric layers, dependencies point inward only:

```
        ┌─────────────────────────────────────────────┐
        │                Presentation                  │  Compose UI, ViewModels,
        │   (Jetpack Compose · Material 3 · MVVM)      │  UI state, navigation
        ├─────────────────────────────────────────────┤
        │                  Domain                      │  Entities, UseCases,
        │     (pure Kotlin · no Android deps)          │  Repository interfaces
        ├─────────────────────────────────────────────┤
        │                   Data                       │  Repo impls, data sources,
        │  (MediaStore · SAF · DocumentFile · Room)    │  Room, DataStore, mappers
        └─────────────────────────────────────────────┘
```

- **Domain** is pure Kotlin: entities (`FileItem`, `StorageVolume`, `MediaCategory`),
  use cases (`ListDirectoryUseCase`, `CopyFilesUseCase`, …), and repository
  **interfaces**. No Android imports — fully unit-testable.
- **Data** implements the repository interfaces over Android sources: a
  `FileSystemDataSource` (java.io for owned/app dirs), a `SafDataSource`
  (DocumentFile/SAF for tree access), a `MediaStoreDataSource` (categories &
  thumbnails), Room (favorites/recents/cache index), and DataStore (settings).
- **Presentation** holds `ViewModel`s exposing immutable UI state via `StateFlow`,
  consuming use cases; Compose screens render state and emit events upward (UDF).

## 2. Patterns

- **Unidirectional Data Flow (UDF):** `UiState` (immutable) ← ViewModel ← UseCase
  ← Repository; user intents flow up as events.
- **Repository pattern:** one interface per domain concern; data layer chooses the
  right source (java.io vs SAF vs MediaStore) behind the interface.
- **Result wrapper:** use cases return a sealed `Result<T>` (Success/Error/Loading
  where relevant) so the UI renders errors without exceptions crossing layers.
- **Mappers:** data DTOs/cursors ↔ domain entities live in the data layer.

## 3. Concurrency

- **Coroutines + Flow** throughout. Repositories expose `Flow` for streams
  (directory contents, search results) and `suspend` for one-shot calls.
- Dispatchers injected (`@IoDispatcher`, `@DefaultDispatcher`) for testability.
- Heavy/long work (large copy, zip, hashing) runs in **WorkManager** workers with
  a foreground service + progress, decoupled from the UI lifecycle.

## 4. Dependency Injection

- **Hilt** for DI. Modules: `AppModule`, `DispatcherModule`, `DatabaseModule`,
  `DataStoreModule`, `RepositoryModule` (binds interfaces → impls),
  `WorkManagerModule`. ViewModels via `@HiltViewModel`.

## 5. Storage Strategy (decision matrix)

| Scope                              | API used                         |
|------------------------------------|----------------------------------|
| App-owned dirs / cache             | `java.io.File`                   |
| Shared media (images/video/audio)  | `MediaStore`                     |
| Arbitrary user tree (SD/USB/other) | SAF tree URI + `DocumentFile`    |
| Full-tree management (opt-in)      | `MANAGE_EXTERNAL_STORAGE` gated  |
| Settings                           | Jetpack `DataStore` (Preferences)|
| Favorites/recents/index            | `Room`                           |

A `StorageRepository` abstracts these so use cases never branch on API level.

## 6. Navigation

**Navigation Compose** with a typed route sealed hierarchy. Single-activity app;
`MainActivity` hosts the `NavHost`. Deep links reserved for future. See the
**Navigation Flow** document.

## 7. Persistence (Room)

Entities: `FavoriteEntity`, `RecentEntity`, optional `ThumbnailIndexEntity`.
DAOs expose `Flow` queries. Migrations versioned from v1; schema exported for
migration tests.

## 8. Theming

Material 3 with a custom Filora color scheme; **dynamic color** on API 31+.
Typography/shape tokens centralized. Light/Dark/System via DataStore-backed state.

## 9. Error Handling

- Domain errors modeled as sealed types (`OperationError.PermissionDenied`,
  `.Conflict`, `.NotFound`, `.OutOfSpace`, `.Unknown`).
- UI maps errors to snackbars/dialogs; no raw stack traces surfaced to users.

## 10. Testing Architecture

- **Unit:** domain use cases & mappers (JUnit, Turbine for Flow, MockK).
- **Repository:** fakes for data sources; Room in-memory DB tests.
- **UI:** Compose UI tests for key flows (browse, select, operate).
- **Instrumented:** SAF/MediaStore smoke tests on emulator matrix.

## 11. Build & Tooling

- Gradle Kotlin DSL, version catalog (`libs.versions.toml`), modular `:app`
  + feature/core modules (see **Module Breakdown**).
- R8 minification + resource shrinking for release; baseline profiles for startup.
- CI: build → ktlint/detekt → unit tests → assemble; signed AAB for release track.

## 12. Key Technical Decisions (and why)

1. **SAF + DocumentFile as the default for non-media trees** over requesting
   broad storage — keeps us Play-policy-safe and least-privilege.
2. **WorkManager for long ops** — survives process death, gives reliable progress.
3. **Domain layer free of Android** — enables fast JVM unit tests and protects
   business rules from platform churn.
4. **Single-activity + Compose Navigation** — simpler back-stack and state handling.
