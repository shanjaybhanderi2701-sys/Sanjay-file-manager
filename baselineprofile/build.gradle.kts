// :baselineprofile — Macrobenchmark test module that GENERATES the app's baseline
// profile and measures cold-start time (NFR-1.1). It is a `com.android.test` module,
// so it is never shipped: it drives the installed app via UiAutomator on a device.
//
// Generate the profile:   ./gradlew :baselineprofile:generateBaselineProfile
// Measure startup:         ./gradlew :baselineprofile:connectedBenchmarkAndroidTest
//
// Both require a connected device/emulator AND a feature-complete, launchable app
// (gated on T6.5). Until then this module compiles but its on-device tasks are no-ops.
plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.androidx.baselineprofile)
}

android {
    namespace = "com.appblish.filora.baselineprofile"
    compileSdk = libs.versions.compileSdk.get().toInt()

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    defaultConfig {
        // Macrobenchmark requires API 28+ on the test device even though the app minSdk is 24.
        minSdk = 28
        targetSdk = libs.versions.targetSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Drives the "standard" Play-default flavor (least privilege) of the app.
    targetProjectPath = ":app"
    flavorDimensions += "access"
    productFlavors {
        create("standard") { dimension = "access" }
    }

    // Run the generator/benchmark on a rooted emulator or a real device.
    @Suppress("UnstableApiUsage")
    experimentalProperties["android.experimental.self-instrumenting"] = true
}

baselineProfile {
    // Generate against the "benchmark" build type the plugin creates on :app.
    useConnectedDevices = true
}

dependencies {
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.espresso.core)
    implementation(libs.androidx.test.uiautomator)
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.test.runner)
}
