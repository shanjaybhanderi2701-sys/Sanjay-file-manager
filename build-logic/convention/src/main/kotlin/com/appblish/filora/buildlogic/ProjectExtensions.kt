package com.appblish.filora.buildlogic

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

/** Accessor for the shared `libs` version catalog from inside convention plugins. */
internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

/** Single source of truth for SDK levels, read from the version catalog. */
internal val VersionCatalog.minSdkVersion: Int
    get() = findVersion("minSdk").get().requiredVersion.toInt()

internal val VersionCatalog.targetSdkVersion: Int
    get() = findVersion("targetSdk").get().requiredVersion.toInt()

internal val VersionCatalog.compileSdkVersion: Int
    get() = findVersion("compileSdk").get().requiredVersion.toInt()
