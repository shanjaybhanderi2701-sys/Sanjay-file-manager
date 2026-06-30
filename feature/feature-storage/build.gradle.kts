plugins {
    alias(libs.plugins.filora.android.feature)
}

android {
    namespace = "com.appblish.filora.feature.storage"
}

dependencies {
    // Per-category icons (image/video/audio/docs/apk/zip) on the breakdown rows.
    implementation(libs.androidx.compose.material.icons.extended)
}
