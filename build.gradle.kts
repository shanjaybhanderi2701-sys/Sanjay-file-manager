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
}

// Apply ktlint + detekt to every module from one place.
subprojects {
    apply(plugin = rootProject.libs.plugins.ktlint.get().pluginId)
    apply(plugin = rootProject.libs.plugins.detekt.get().pluginId)

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
