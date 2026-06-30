plugins {
    alias(libs.plugins.filora.kotlin.library)
}

dependencies {
    api(libs.kotlinx.coroutines.core)
    // javax.inject only — pure annotations, keeps this module Android/Hilt-free.
    api(libs.javax.inject)
}
