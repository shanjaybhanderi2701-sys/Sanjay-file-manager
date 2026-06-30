package com.appblish.filora.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

/**
 * Configures the Android + Kotlin baseline shared by every Android module:
 * SDK levels, Java 17, and Kotlin compiler options.
 */
internal fun Project.configureKotlinAndroid(
    commonExtension: CommonExtension<*, *, *, *, *, *>,
) {
    commonExtension.apply {
        compileSdk = libs.compileSdkVersion

        defaultConfig {
            minSdk = libs.minSdkVersion
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }

        lint {
            // Run lint on every PR and surface results, but don't gate the first
            // foundation merge on it — a lint baseline is introduced in a follow-up
            // so new regressions can be caught without drowning in initial noise.
            abortOnError = false
            warningsAsErrors = false
            checkDependencies = true
        }
    }

    configure<KotlinAndroidProjectExtension> {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
            // Treat warnings strictly in CI but keep local builds usable.
            allWarningsAsErrors = providers.gradleProperty("warningsAsErrors")
                .map(String::toBoolean).orElse(false)
            freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
        }
    }
}
