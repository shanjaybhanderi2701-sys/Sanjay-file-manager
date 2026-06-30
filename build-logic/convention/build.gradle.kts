plugins {
    `kotlin-dsl`
}

group = "com.appblish.filora.buildlogic"

// Convention plugins target the same JDK as the rest of the project.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "filora.android.application"
            implementationClass = "com.appblish.filora.buildlogic.AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "filora.android.library"
            implementationClass = "com.appblish.filora.buildlogic.AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "filora.android.compose"
            implementationClass = "com.appblish.filora.buildlogic.AndroidComposeConventionPlugin"
        }
        register("androidFeature") {
            id = "filora.android.feature"
            implementationClass = "com.appblish.filora.buildlogic.AndroidFeatureConventionPlugin"
        }
        register("androidHilt") {
            id = "filora.android.hilt"
            implementationClass = "com.appblish.filora.buildlogic.HiltConventionPlugin"
        }
        register("androidRoom") {
            id = "filora.android.room"
            implementationClass = "com.appblish.filora.buildlogic.RoomConventionPlugin"
        }
        register("kotlinLibrary") {
            id = "filora.kotlin.library"
            implementationClass = "com.appblish.filora.buildlogic.KotlinLibraryConventionPlugin"
        }
    }
}
