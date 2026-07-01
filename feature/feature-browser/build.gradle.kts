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
}
