import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension

// Root build file. Plugins are declared `apply false` here and applied per-module
// (directly or via Filora convention plugins in `build-logic`).
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    // Declared versionless on the root classpath so the :baselineprofile module can
    // apply com.android.test without re-requesting a version (which conflicts with the
    // already-loaded AGP plugins). Same apply-false pattern as the other AGP plugins.
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.androidx.baselineprofile) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
    // T170 — applied to the root project so it can aggregate per-module coverage.
    alias(libs.plugins.kover)
}

// T170 (M16) — Kover coverage aggregation.
// Measured modules apply Kover and are aggregated into a single root report. The gate
// THRESHOLD VALUES were a Product Manager decision (APP-36 interaction c51ac274),
// resolved 2026-07-01 → Option A: 70% line for core-domain/core-data, 50% line overall,
// ENFORCED (fail-under). `koverVerify` is a blocking CI gate (continue-on-error dropped).
// :app and :baselineprofile are intentionally excluded (product flavors / test-only
// module complicate variant selection and carry little measurable logic).
val koverMeasuredModules =
    listOf(
        ":core:core-common",
        ":core:core-domain",
        ":core:core-data",
        ":core:core-database",
        ":core:core-ui",
        ":feature:feature-home",
        ":feature:feature-browser",
        ":feature:feature-search",
        ":feature:feature-media",
        ":feature:feature-storage",
        ":feature:feature-settings",
    )
val koverStrictModules = listOf(":core:core-domain", ":core:core-data")

// Apply ktlint + detekt to every module from one place.
subprojects {
    apply(plugin = rootProject.libs.plugins.ktlint.get().pluginId)
    apply(plugin = rootProject.libs.plugins.detekt.get().pluginId)

    if (path in koverMeasuredModules) {
        apply(plugin = rootProject.libs.plugins.kover.get().pluginId)
        if (path in koverStrictModules) {
            extensions.configure<KoverProjectExtension> {
                reports {
                    verify {
                        // ENFORCED — PM APP-36 Option A: 70% line for core-domain/core-data.
                        rule("Line coverage (core-domain/core-data — PM APP-36 Option A)") {
                            minBound(70)
                        }
                    }
                }
            }
        }
    }

    extensions.configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        version.set("1.4.1")
        android.set(true)
        ignoreFailures.set(false)
        filter {
            exclude { it.file.path.contains("/build/") }
        }
    }

    extensions.configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        config.setFrom(rootProject.files("config/detekt/detekt.yml"))
        buildUponDefaultConfig = true
        parallel = true
    }
}

// T017 — module dependency-rule check. Enforces the load-bearing layering rule that
// feature modules never depend on each other (see docs/architecture/module-design.md).
// Kept as a simple, readable task; pairs with the build-level guards in the
// convention plugins. Runs in CI alongside lint/detekt.
tasks.register("checkModuleDependencies") {
    group = "verification"
    description = "Fails the build if any feature-* module depends on another feature-* module."
    doLast {
        val violations = mutableListOf<String>()
        rootProject.subprojects
            .filter { it.path.startsWith(":feature:") }
            .forEach { project ->
                project.configurations.forEach { configuration ->
                    configuration.dependencies
                        .filterIsInstance<org.gradle.api.artifacts.ProjectDependency>()
                        .forEach { dependency ->
                            val depPath = dependency.path
                            if (depPath.startsWith(":feature:") && depPath != project.path) {
                                violations += "${project.path} -> $depPath (feature -> feature is not allowed)"
                            }
                        }
                }
            }
        if (violations.isNotEmpty()) {
            throw org.gradle.api.GradleException(
                "Module dependency-rule violations:\n" + violations.joinToString("\n") { "  - $it" },
            )
        }
        logger.lifecycle("Module dependency rules OK (${rootProject.subprojects.size} modules checked).")
    }
}

// T170 — pull every measured module into the aggregated root Kover report and wire the
// overall (aggregated) gate. `koverXmlReport` / `koverHtmlReport` produce the CI artifact;
// `koverVerify` enforces the rules (blocking in CI — PM APP-36 Option A, T172 sign-off).
dependencies {
    koverMeasuredModules.forEach { add("kover", project(it)) }
}

kover {
    reports {
        total {
            verify {
                // ENFORCED — PM APP-36 Option A: 50% overall line coverage.
                rule("Overall line coverage (PM APP-36 Option A)") {
                    minBound(50)
                }
            }
        }
    }
}
