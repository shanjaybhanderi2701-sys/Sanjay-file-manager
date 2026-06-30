plugins {
    alias(libs.plugins.filora.android.feature)
}

android {
    namespace = "com.appblish.filora.feature.media"
}

dependencies {
    // Category-hub tile icons (image/video/audio/docs/apk/zip) live in the extended set.
    implementation(libs.androidx.compose.material.icons.extended)
}
