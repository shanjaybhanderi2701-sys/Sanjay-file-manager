package com.appblish.filora.buildlogic

import com.google.devtools.ksp.gradle.KspExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * Adds Room (with KSP) and exports the schema JSON to `<module>/schemas` so Room's
 * [androidx.room.testing.MigrationTestHelper] can validate every version. Migration
 * tests are instrumented, so the schema dir is wired into the androidTest assets in
 * the consuming module's `build.gradle.kts`.
 */
class RoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.google.devtools.ksp")

        extensions.configure<KspExtension> {
            arg("room.generateKotlin", "true")
            arg("room.schemaLocation", "$projectDir/schemas")
        }

        dependencies {
            add("implementation", libs.findLibrary("androidx-room-runtime").get())
            add("implementation", libs.findLibrary("androidx-room-ktx").get())
            add("ksp", libs.findLibrary("androidx-room-compiler").get())
            // The androidTest runner (+ AndroidJUnit4) is provided by
            // AndroidLibraryConventionPlugin, which every Room module also applies.
            add("androidTestImplementation", libs.findLibrary("androidx-room-testing").get())
        }
    }
}
