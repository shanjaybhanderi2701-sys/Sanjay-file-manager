plugins {
    alias(libs.plugins.filora.android.library)
    alias(libs.plugins.filora.android.compose)
    alias(libs.plugins.filora.android.hilt)
    // T171 (M16) — JVM screenshot verification tasks (recordRoborazzi* / verifyRoborazzi*).
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "com.appblish.filora.core.ui"

    // Roborazzi renders Compose on the JVM through Robolectric, which needs merged
    // Android resources available to unit tests.
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    api(project(":core:core-common"))

    implementation(libs.androidx.core.ktx)
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.material.icons.extended)
    api(libs.androidx.lifecycle.runtime.compose)

    // Coil powers MediaThumbnail; AsyncImage is re-exported so feature modules can use it.
    api(libs.coil.compose)
    implementation(libs.coil.video)

    // T171 — JVM screenshot harness (Roborazzi + Robolectric + Compose test rule).
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.compose.ui.test.manifest)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
}
