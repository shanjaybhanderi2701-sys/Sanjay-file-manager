plugins {
    alias(libs.plugins.filora.android.feature)
}

android {
    namespace = "com.appblish.filora.feature.browser"
}

dependencies {
    // Action-bar icons (move/copy/zip/select-all) live in the extended icon set.
    implementation(libs.androidx.compose.material.icons.extended)

    // ACTION_OPEN_DOCUMENT_TREE launcher for the SAF copy/move destination picker (FR-3.2).
    implementation(libs.androidx.activity.compose)

    // T165 (M16) — instrumented Compose UI test for BrowserContent. The compose
    // convention already provides the compose ui-test rule + BOM on androidTest; add the
    // AndroidX test runner/ext so AndroidJUnit4 + InstrumentationRegistry resolve.
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
}
