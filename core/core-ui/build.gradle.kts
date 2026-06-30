plugins {
    alias(libs.plugins.filora.android.library)
    alias(libs.plugins.filora.android.compose)
    alias(libs.plugins.filora.android.hilt)
}

android {
    namespace = "com.appblish.filora.core.ui"
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
}
