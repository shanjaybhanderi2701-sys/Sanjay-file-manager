# Contributing to Filora

Thanks for working on Filora. This guide covers the conventions the codebase and
CI enforce. For the module layout and layering read
[`docs/phase-1/architecture.md`](docs/phase-1/architecture.md); for build and run
commands see the [README](README.md).

## Prerequisites

| Tool | Version |
|---|---|
| JDK | 17 |
| Android SDK | compileSdk 35 (min 24, target 35) |
| Gradle | Use the bundled wrapper (`./gradlew`, 8.11.1) — do not install Gradle |

All dependency versions are pinned in
[`gradle/libs.versions.toml`](gradle/libs.versions.toml). **Never hard-code a
version** in a module `build.gradle.kts` — add it to the catalog and reference it.

## Branching & pull requests

- Branch from `main`, named `app-<issue>-<slug>` (e.g. `app-23-m3-file-browser`).
- **One milestone / task group per PR.** Keep PRs small and reviewable.
- CI must be **green** before merge — see the gates below.
- **Architecture-affecting** changes require an Android Architect review.
- **Permission or data-handling** changes also require Security Engineer sign-off.
- Commit messages follow the existing convention:
  `type(scope): summary (APP-### T###)`, e.g.
  `feat(m5): Search screen + filter chips (APP-73 T5.2)`.

## Required checks (run before you push)

CI runs exactly these; run them locally first:

```bash
./gradlew ktlintCheck detekt checkModuleDependencies lint test assembleStandardDebug
```

| Gate | Command | What it enforces |
|---|---|---|
| Formatting | `./gradlew ktlintFormat` then `ktlintCheck` | Kotlin official style (ktlint) |
| Static analysis | `./gradlew detekt` | Code smells; `maxIssues: 0` — the build fails on any finding |
| Module rules | `./gradlew checkModuleDependencies` | `feature-*` isolation + pure-JVM cores (see below) |
| Android lint | `./gradlew lint` | Missing translations, resource issues, etc. |
| Tests | `./gradlew test` | JVM unit tests across all modules |

> Note: `app` is flavored, so app-level Kotlin compiles via
> `compileStandardDebugKotlin`; pure-JVM cores compile via `compileKotlin`;
> Android library modules via `compileDebugKotlin`.

## Architecture rules the build enforces

- **`feature-*` modules must not depend on each other.** Cross-feature navigation
  is resolved in `app` via typed routes.
- **`core-common` and `core-domain` are pure Kotlin** — no `android.*`, no Hilt,
  no Compose.
- **Features depend on `core-domain` interfaces only**, never on `core-data` /
  `core-database`. Implementations are bound into the Hilt graph in `app`.
- New modules are wired through the **convention plugins** in `build-logic`
  (`filora.android.library`, `filora.android.compose`, `filora.android.feature`,
  `filora.android.hilt`, `filora.android.room`, `filora.kotlin.library`). Apply a
  plugin — do not copy raw Android/Kotlin config into a module.

## Coding standards

- **Architecture:** Clean Architecture + MVVM. UI state is immutable; ViewModels
  expose `StateFlow<XUiState>` and receive events. Use cases return
  `com.appblish.filora.core.common.result.Result<T>` — exceptions never cross a
  layer boundary.
- **Coroutines:** inject dispatchers via `@IoDispatcher` / `@DefaultDispatcher`;
  never hard-code `Dispatchers.*` in domain/data classes.
- **DI:** Hilt; modules suffixed `Module`, installed in the narrowest component.
- **Naming:** use cases `VerbNounUseCase`; ViewModels `XViewModel`; repository
  interfaces live in `core-domain`, impls are `XRepositoryImpl` in `core-data`.
- **Tests:** mirror the package under `src/test` with a `Test` suffix. Prefer
  hand-written fakes of the `core-domain` seams over mocking framework internals;
  JUnit + Truth + Turbine + MockK for JVM, Compose UI tests for flows.

## Localization & RTL (required, NFR-7)

- **No hard-coded user-facing strings.** Every visible string comes from
  `stringResource(R.string.…)`; error state carries a `@StringRes`
  (`errorMessageRes`), not raw English.
- Provide translations in `res/values-<locale>/strings.xml`; the app ships
  English + Arabic. `lint` fails on `MissingTranslation`.
- Use start/end (not left/right) constraints and test in RTL. See
  [`docs/phase-1/localization-rtl.md`](docs/phase-1/localization-rtl.md).

## Documentation

Keep docs in sync with shipped behavior. When a change alters user-visible
behavior, update the relevant page under [`docs/phase-1/`](docs/phase-1/) and add
a line to [`CHANGELOG.md`](CHANGELOG.md). Drift is a bug.
