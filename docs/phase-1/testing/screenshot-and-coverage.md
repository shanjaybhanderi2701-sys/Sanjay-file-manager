# M16 — Screenshot & Coverage Harnesses (APP-122)

Covers the **JVM-only** slice of the M16 CI/testing bundle: **T170** (Kover coverage
gate) and **T171** (Roborazzi screenshot tests). Both run on the ordinary
`ubuntu-latest` CI lane — no emulator required.

The instrumented/emulator slice of APP-122 (**T165** compose UI tests, **T166** E2E
nav flow, **T168** permission-flow instrumentation) is **blocked on APP-111** (the CI
instrumented emulator matrix — API30 SAF / API35 phone / API35 tablet) and is not part
of this document.

---

## T170 — Coverage (Kover)

Kover is applied to the root project for aggregation and to every measured module:

- `core-common`, `core-domain`, `core-data`, `core-database`, `core-ui`
- all `feature-*` modules

`:app` and `:baselineprofile` are intentionally excluded — product flavors and the
test-only benchmark module complicate variant selection and hold little measurable
logic.

### Tasks

| Task | Purpose |
| --- | --- |
| `./gradlew koverHtmlReport` | Human-readable aggregated report → `build/reports/kover/html/` |
| `./gradlew koverXmlReport`  | Machine-readable report (CI artifact) → `build/reports/kover/report.xml` |
| `./gradlew koverVerify`     | Enforces the coverage rules |

### Thresholds — PENDING PM decision

The gate **values** are a Product Manager decision, tracked on the APP-36 thread
interaction **`c51ac274`**. Until PM confirms, the proposed defaults from the APP-122
spec are wired as **placeholders** and the CI gate is **non-blocking**:

| Scope | Placeholder (line coverage) |
| --- | --- |
| Overall (aggregated) | **50%** |
| `core-domain`, `core-data` | **70%** |

In CI the `coverage` job runs `koverVerify` with `continue-on-error: true`. **To turn
the gate on once PM confirms:** update the `minBound(...)` values in
`build.gradle.kts` and remove `continue-on-error` from the *Kover verify* step in
`.github/workflows/ci.yml`.

---

## T171 — Screenshot tests (Roborazzi + Robolectric)

Roborazzi renders real Compose on the JVM through Robolectric, so screenshots run in
the standard unit-test lane (`testDebugUnitTest`) with **no emulator**.

Current coverage — `core-ui` shared components, in
`core/core-ui/src/test/kotlin/.../screenshot/ComponentScreenshotTest.kt`:

- **FileRow** (content) — light / dark / dynamic-color
- **GridTile** (content) — light / dark
- **ProgressBarRow** (loading) — determinate (light) + indeterminate (dark)
- **EmptyState** (empty) — light / dark / dynamic-color
- **EmptyState** (error variant) — light

### Recording & verifying goldens

Screenshot tests need committed golden PNGs. On first setup (or after an intentional
visual change) record them, then commit:

```bash
./gradlew :core:core-ui:recordRoborazziDebug     # writes core/core-ui/src/test/screenshots/*.png
git add core/core-ui/src/test/screenshots
```

CI enforces them in the `screenshots` job:

```bash
./gradlew :core:core-ui:verifyRoborazziDebug     # fails on any pixel diff; diffs uploaded as artifact
```

> **Note (sandbox limitation):** the goldens are **not yet committed** — they must be
> recorded from an environment with a JDK/Gradle (the authoring sandbox has neither).
> Run `recordRoborazziDebug` in CI or locally and commit the output before the
> `screenshots` CI job can pass. The plain `test` lane renders the components as a
> smoke check regardless (Roborazzi is a no-op there without a record/verify task).
