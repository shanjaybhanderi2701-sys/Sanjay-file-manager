# Filora

[![CI](https://github.com/shanjaybhanderi2701-sys/Sanjay-file-manager/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/shanjaybhanderi2701-sys/Sanjay-file-manager/actions/workflows/ci.yml)

An Android-native file manager (Jetpack Compose + Kotlin) by appblish.

This is the engineering README — how to build, run, and contribute. Product and
planning context lives under [`docs/phase-1/`](docs/phase-1/); the as-built
architecture is [`docs/phase-1/architecture.md`](docs/phase-1/architecture.md),
and contributor conventions are in [`CONTRIBUTING.md`](CONTRIBUTING.md).

---

## Features

Filora is organized around six top-level areas, each backed by its own
`feature-*` module:

- **Home** — a dashboard of storage volumes, media category counts, recents, and
  pinned favorites, with tap-through to the relevant screen.
- **Browse** — navigate the file tree with list/grid layouts, sort, hidden-file
  toggle, and pull-to-refresh; multi-select with a batch action bar; create,
  rename, delete, copy/move (conflict-aware), and share via `FileProvider`.
- **Search** — streaming, cancelable name search across the tree with removable
  type / size / date filter chips; tap a result to jump to its location.
- **Media** — seven category hubs (images, video, audio, docs, etc.) with Coil
  thumbnails, a category detail grid, and open/play/share intents.
- **Storage** — per-volume used/free breakdown, by-category sizing, a
  storage-story hero band, and a largest-files view with open/share/delete.
- **Favorites** — pin/unpin files and folders from Browse and Media, surfaced on
  the Home strip (Room-backed).
- **Settings** — theme, dynamic color, default list/grid layout, sort order, and
  hidden-file preference, persisted via DataStore and applied live.

Archive support (ZIP compress/extract) and file operations run as WorkManager
foreground jobs with progress. See the
[Phase-1 changelog](CHANGELOG.md) for what shipped in each milestone.

## Requirements

| Tool | Version |
|---|---|
| JDK | 17 |
| Android SDK | compileSdk **35** (min **24**, target **35**) |
| Gradle | 8.11.1 (via the wrapper — do not install separately) |
| Kotlin / AGP | 2.1.0 / 8.7.3 (see [`gradle/libs.versions.toml`](gradle/libs.versions.toml)) |

Use the bundled wrapper (`./gradlew`). All dependency versions are pinned in the
version catalog — never hard-code a version in a module `build.gradle.kts`.

## Build & run

```bash
# Assemble the default (Play) flavor, debug build
./gradlew assembleStandardDebug

# Install on a connected device/emulator
./gradlew installStandardDebug

# Full local check (what CI runs)
./gradlew ktlintCheck detekt checkModuleDependencies lint test assembleStandardDebug
```

The app launches to the **Home** dashboard. Two product flavors:

- **standard** — the Play default. Least-privilege; **no** all-files access.
- **fullaccess** — opt-in only; declares `MANAGE_EXTERNAL_STORAGE`, gated by
  `BuildConfig.FULL_ACCESS_SUPPORTED`. Never ship this as the default.

## Module map

```
app                 Application, MainActivity, NavHost, DI assembly, flavors
build-logic         Convention plugins (the single place build config lives)
core/
  core-common       Result, OperationError, dispatchers, formatters   (pure JVM)
  core-domain       Entities + repository interfaces                  (pure JVM)
  core-ui           FiloraTheme (M3 + dynamic color), shared composables
  core-database     Room db, entities, DAOs, DI
  core-data         Repository impls, data sources, WorkManager operations
feature/
  feature-home | -browser | -search | -media | -storage | -settings
```

**Dependency rules (CI-enforced):**

- `feature-*` modules **never** depend on each other (`checkModuleDependencies`).
- `core-common` / `core-domain` are **pure Kotlin** — no `android.*`, no Hilt.
- `app` is the only module that assembles the Hilt graph and owns navigation.

New modules are wired through the **convention plugins** in `build-logic`
(`filora.android.library`, `filora.android.compose`, `filora.android.feature`,
`filora.android.hilt`, `filora.android.room`, `filora.kotlin.library`). Apply a
plugin; don't copy raw Android/Kotlin config into a module.

## Branching & PRs

- Branch from `main`: `app-<issue>-<slug>` (e.g. `app-21-m1-project-foundation`).
- One milestone/task group per PR; keep PRs reviewable.
- CI must be green (ktlint, detekt, module rules, lint, tests, assemble) before merge.
- Architecture-affecting changes get an Android Architect review; permission/data
  changes also require Security Engineer sign-off.

## Coding standards

- **Kotlin official style**, enforced by **ktlint**; smells by **detekt**. Run
  `./gradlew ktlintFormat` to auto-fix formatting.
- **Architecture:** Clean Architecture + MVVM. UI state is immutable; ViewModels
  expose `StateFlow<XUiState>` and receive events; use cases return
  `com.appblish.filora.core.common.result.Result<T>` — exceptions never cross layers.
- **Coroutines:** inject dispatchers via `@IoDispatcher` / `@DefaultDispatcher`;
  never hard-code `Dispatchers.*` in domain/data classes.
- **DI:** Hilt; modules suffixed `Module`, installed in the narrowest component.
- **Naming:** UseCases `VerbNounUseCase`; ViewModels `XViewModel`; repo interfaces
  in `core-domain`, impls `XRepositoryImpl` in `core-data`. Tests mirror the
  package under `src/test` with a `Test` suffix.
- **Tests:** JUnit + Truth + Turbine + MockK for JVM; Compose UI tests for flows.

## Repository layout reference

See [`docs/phase-1/05-package-structure.md`](docs/phase-1/05-package-structure.md)
for the full per-module source tree and naming conventions.
