plugins {
    alias(libs.plugins.filora.android.library)
    alias(libs.plugins.filora.android.hilt)
}

android {
    namespace = "com.appblish.filora.core.data"
}

dependencies {
    implementation(project(":core:core-common"))
    implementation(project(":core:core-domain"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)
}
