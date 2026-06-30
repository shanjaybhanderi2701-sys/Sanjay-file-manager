plugins {
    alias(libs.plugins.filora.kotlin.library)
}

dependencies {
    api(project(":core:core-common"))
    api(libs.kotlinx.coroutines.core)
}
