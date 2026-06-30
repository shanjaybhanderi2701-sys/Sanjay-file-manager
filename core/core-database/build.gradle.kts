plugins {
    alias(libs.plugins.filora.android.library)
    alias(libs.plugins.filora.android.hilt)
    alias(libs.plugins.filora.android.room)
}

android {
    namespace = "com.appblish.filora.core.database"

    // Package the exported Room schemas into the androidTest APK so
    // MigrationTestHelper can open each historical schema version.
    sourceSets.getByName("androidTest") {
        assets.srcDir("$projectDir/schemas")
    }
}

dependencies {
    implementation(project(":core:core-common"))
    implementation(libs.kotlinx.coroutines.android)

    // Truth for the instrumented migration test (the library convention plugin
    // only wires Truth into the JVM `test` source set).
    androidTestImplementation(libs.truth)
}
