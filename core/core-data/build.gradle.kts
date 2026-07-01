plugins {
    alias(libs.plugins.filora.android.library)
    alias(libs.plugins.filora.android.hilt)
}

android {
    namespace = "com.appblish.filora.core.data"

    // Robolectric needs the module's merged resources + a generated test config to
    // resolve `R.string` (notification copy) and build `Notification`s under the JVM,
    // for the WorkManager worker-execution tests (T167). No emulator required.
    testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {
    implementation(project(":core:core-common"))
    implementation(project(":core:core-database"))
    implementation(project(":core:core-domain"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)

    // Worker-execution tests (T167): Robolectric JVM harness + WorkManager test builders
    // + the shared in-memory FileRepository/fileTree fixtures from :core:core-testing.
    testImplementation(project(":core:core-testing"))
    testImplementation(libs.androidx.work.testing)
    testImplementation(libs.robolectric)
}
