plugins {
    alias(libs.plugins.filora.android.feature)
}

android {
    namespace = "com.appblish.filora.feature.browser"
}

dependencies {
    // Action-bar icons (move/copy/zip/select-all) live in the extended icon set.
    implementation(libs.androidx.compose.material.icons.extended)
}
