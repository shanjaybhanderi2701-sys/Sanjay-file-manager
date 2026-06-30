package com.appblish.filora.core.data.storage

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds the SAF tree-access abstraction of the data layer to its Android
 * implementation. Kept as a `@Binds` interface so consumers (and tests) depend on
 * the [SafTreeAccess] contract, not the `ContentResolver` plumbing behind it.
 *
 * Volume-enumeration bindings (and the `StorageManager` provider they need) are
 * contributed by the storage-volume slice (T1.2), so this module stays scoped to
 * the SAF grant flow (T1.3).
 */
@Module
@InstallIn(SingletonComponent::class)
internal interface StorageDataModule {
    @Binds
    @Singleton
    fun bindSafTreeAccess(impl: AndroidSafTreeAccess): SafTreeAccess
}
