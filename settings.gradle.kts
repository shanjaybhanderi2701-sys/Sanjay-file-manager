pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Filora"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":app")

include(":core:core-common")
include(":core:core-domain")
include(":core:core-ui")
include(":core:core-data")
include(":core:core-database")

include(":feature:feature-home")
include(":feature:feature-browser")
include(":feature:feature-search")
include(":feature:feature-media")
include(":feature:feature-storage")
include(":feature:feature-settings")

// M7 performance hardening: baseline-profile producer + startup macrobenchmark (NFR-1.1).
include(":baselineprofile")
