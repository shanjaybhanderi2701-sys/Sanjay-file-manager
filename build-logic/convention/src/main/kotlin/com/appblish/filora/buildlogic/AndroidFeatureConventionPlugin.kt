package com.appblish.filora.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project

/**
 * One plugin for every `feature-*` module. Composes library + compose + hilt and
 * wires the standard feature dependencies. Crucially, feature modules get
 * `core-*` only — never another feature — which enforces the no-feature-to-feature
 * rule at the build level.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("filora.android.library")
            apply("filora.android.compose")
            apply("filora.android.hilt")
        }

        dependencies {
            add("implementation", project(":core:core-common"))
            add("implementation", project(":core:core-domain"))
            add("implementation", project(":core:core-ui"))

            add("implementation", libs.findLibrary("androidx-core-ktx").get())
            add("implementation", libs.findLibrary("androidx-lifecycle-runtime-compose").get())
            add("implementation", libs.findLibrary("androidx-lifecycle-viewmodel-compose").get())
            add("implementation", libs.findLibrary("androidx-navigation-compose").get())
            add("implementation", libs.findLibrary("androidx-hilt-navigation-compose").get())
            add("implementation", libs.findLibrary("kotlinx-coroutines-android").get())
        }
    }
}
