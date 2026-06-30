plugins {
    alias(libs.plugins.filora.android.library)
    alias(libs.plugins.filora.android.hilt)
    alias(libs.plugins.filora.android.room)
}

android {
    namespace = "com.appblish.filora.core.database"
}

dependencies {
    implementation(project(":core:core-common"))
    implementation(libs.kotlinx.coroutines.android)
}
