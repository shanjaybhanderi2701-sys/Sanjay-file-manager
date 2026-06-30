plugins {
    alias(libs.plugins.filora.kotlin.library)
}

dependencies {
    api(project(":core:core-common"))
    api(libs.kotlinx.coroutines.core)

    // Shared fake repositories + file-tree DSL (T169). The fixtures live in core-testing's
    // main source set; consuming it from `test` keeps the dependency cycle to test configs only.
    testImplementation(project(":core:core-testing"))
}
