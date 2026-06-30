plugins {
    alias(libs.plugins.filora.android.feature)
}

android {
    namespace = "com.appblish.filora.feature.home"
}

dependencies {
    // Category tile icons (image/video/audio/docs/apk/zip) live in the extended set.
    implementation(libs.androidx.compose.material.icons.extended)
}
