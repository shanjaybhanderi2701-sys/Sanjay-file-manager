package com.appblish.filora.core.data.thumbnail

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Wires the thumbnail engine (T4.2) into the graph. The cache is a process-wide
 * [Singleton] so it survives navigation (FR-6.2), sized from the heap at startup
 * via [ThumbnailBudget] so it scales with device RAM (NFR-9.2 bounded cache).
 * Consumers depend on the [ThumbnailLoader] contract, not the decode plumbing.
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class ThumbnailModule {
    @Binds
    @Singleton
    abstract fun bindThumbnailLoader(impl: AndroidThumbnailLoader): ThumbnailLoader

    companion object {
        @Provides
        @Singleton
        fun provideThumbnailCache(): ThumbnailCache =
            ThumbnailCache(ThumbnailBudget.maxBytes(Runtime.getRuntime().maxMemory()))
    }
}
