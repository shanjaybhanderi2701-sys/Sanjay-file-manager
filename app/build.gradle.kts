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
        // Custom runner swaps in HiltTestApplication so the M4 smoke test can
        // launch the real @AndroidEntryPoint MainActivity with the DI graph.
        testInstrumentationRunner = "com.appblish.filora.HiltTestRunner"
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

    // Release signing is opt-in via gradle properties or the FILORA_RELEASE_* env
    // vars so keystore secrets never live in the repo. When unset (local dev / PR
    // CI) the release build falls back to the auto-generated debug keystore below,
    // so `assembleStandardRelease` / `bundleStandardRelease` still produce an
    // installable, minified artifact for the NFR-9.1 size check.
    fun prop(name: String): String? = providers.gradleProperty(name).orNull ?: System.getenv(name)

    val releaseStoreFile = prop("FILORA_RELEASE_STORE_FILE")
    val hasReleaseKeystore = releaseStoreFile != null && file(releaseStoreFile).exists()
    if (hasReleaseKeystore) {
        signingConfigs.create("release") {
            storeFile = file(releaseStoreFile!!)
            storePassword = prop("FILORA_RELEASE_STORE_PASSWORD")
            keyAlias = prop("FILORA_RELEASE_KEY_ALIAS")
            keyPassword = prop("FILORA_RELEASE_KEY_PASSWORD")
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            // NFR-9.1: R8 code shrink + resource shrink to hold the 12 MB budget.
            isMinifyEnabled = true
            isShrinkResources = true
            // AC: the shipped release must NOT be debuggable. This is the buildType
            // default, but we pin it so a future edit can't silently flip it on.
            isDebuggable = false
            // Real upload keystore when provided; otherwise debug-signed so the
            // minified artifact still installs in CI without committing secrets.
            signingConfig =
                if (hasReleaseKeystore) {
                    signingConfigs.getByName("release")
                } else {
                    signingConfigs.getByName("debug")
                }
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

    // Instrumented smoke test (M4 T4.6, spec §6): launches the Hilt-wired app and
    // walks Home → category list to prove the milestone integrates end-to-end. The
    // Compose test rule + ui-test-manifest come from the compose convention plugin.
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
}

// NFR-9.1: enforce the ≤ 12 MB release size budget in CI. The minified standard
// release APK is a tight proxy for the AAB base module (single density/abi at
// install time), so we gate on it: build it, then hard-fail if it overflows.
val releaseSizeBudgetBytes = 12L * 1024 * 1024
tasks.register("verifyStandardReleaseSizeBudget") {
    group = "verification"
    description = "Fails the build if the standard release APK exceeds the NFR-9.1 12 MB budget."
    dependsOn("assembleStandardRelease")
    doLast {
        val apkDir = layout.buildDirectory
            .dir("outputs/apk/standard/release")
            .get()
            .asFile
        val apks = apkDir.listFiles { file -> file.extension == "apk" }?.toList().orEmpty()
        check(apks.isNotEmpty()) { "No release APK found in $apkDir — did assembleStandardRelease run?" }
        val apk = apks.maxByOrNull { it.length() }!!
        val sizeMb = apk.length().toDouble() / (1024 * 1024)
        logger.lifecycle("NFR-9.1 release size: ${apk.name} = %.2f MB (budget 12 MB)".format(sizeMb))
        check(apk.length() <= releaseSizeBudgetBytes) {
            "NFR-9.1 size budget exceeded: ${apk.name} is %.2f MB > 12 MB".format(sizeMb)
        }
    }
}
