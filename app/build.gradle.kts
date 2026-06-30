plugins {
    alias(libs.plugins.filora.android.application)
    alias(libs.plugins.filora.android.compose)
    alias(libs.plugins.filora.android.hilt)
    alias(libs.plugins.kotlin.serialization)
    // M7 (NFR-1.1): consumes the generated baseline profile and bakes it into the AAB.
    alias(libs.plugins.androidx.baselineprofile)
}

android {
    namespace = "com.appblish.filora"

    defaultConfig {
        applicationId = "com.appblish.filora"
        versionCode = 1
        versionName = "0.1.0"
        vectorDrawables.useSupportLibrary = true
    }

    buildFeatures {
        buildConfig = true
    }

    // Play-default ("standard", least-privilege) vs opt-in "fullaccess" flavor.
    // See docs/architecture/module-design.md §3 and system-design.md §Permissions.
    // FULL_ACCESS_SUPPORTED gates the MANAGE_EXTERNAL_STORAGE opt-in flow so the
    // Play-default build can never request all-files access.
    flavorDimensions += "access"
    productFlavors {
        create("standard") {
            dimension = "access"
            isDefault = true
            buildConfigField("boolean", "FULL_ACCESS_SUPPORTED", "false")
        }
        create("fullaccess") {
            dimension = "access"
            applicationIdSuffix = ".fullaccess"
            versionNameSuffix = "-fullaccess"
            buildConfigField("boolean", "FULL_ACCESS_SUPPORTED", "true")
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    // Core
    implementation(project(":core:core-common"))
    implementation(project(":core:core-domain"))
    implementation(project(":core:core-ui"))
    implementation(project(":core:core-data"))
    implementation(project(":core:core-database"))

    // Features
    implementation(project(":feature:feature-home"))
    implementation(project(":feature:feature-browser"))
    implementation(project(":feature:feature-search"))
    implementation(project(":feature:feature-media"))
    implementation(project(":feature:feature-storage"))
    implementation(project(":feature:feature-settings"))

    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    // Performance & memory hardening (M7)
    // NFR-1.1: applies the baseline profile at first run for faster cold start.
    implementation(libs.androidx.profileinstaller)
    // NFR-9.2: automatic memory-leak detection in debug builds only. Auto-installs
    // via its own ContentProvider — no init code required.
    debugImplementation(libs.leakcanary.android)
    // Producer of the baseline profile baked in above (see :baselineprofile module).
    baselineProfile(project(":baselineprofile"))

    // Test
    testImplementation(libs.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
