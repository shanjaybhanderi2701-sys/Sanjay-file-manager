plugins {
    alias(libs.plugins.filora.kotlin.library)
}

// Shared test fixtures (fake repositories + a file-tree builder DSL) consumed by
// the unit/use-case/VM/worker tests across modules. Kept as a pure-JVM library so
// the fakes stay platform-free and fast; consumers wire it via
// `testImplementation(project(":core:core-testing"))`.
dependencies {
    api(project(":core:core-domain"))
}
