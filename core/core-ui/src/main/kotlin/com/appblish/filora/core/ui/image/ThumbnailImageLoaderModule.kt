package com.appblish.filora.core.ui.image

import android.content.Context
import coil.ImageLoader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Exposes the thumbnail [ImageLoader] as an app-wide [Singleton] (FR-6.2). Because
 * it lives in the [SingletonComponent], the same instance — and therefore the same
 * memory and disk caches — is shared across every screen and survives navigation.
 *
 * Inject the [ImageLoader] at the Compose root and publish it through
 * [LocalThumbnailImageLoader] so [MediaThumbnail] reuses this cache rather than
 * building a throwaway loader per screen.
 */
@Module
@InstallIn(SingletonComponent::class)
object ThumbnailImageLoaderModule {
    @Provides
    @Singleton
    fun provideThumbnailImageLoader(
        @ApplicationContext context: Context,
    ): ImageLoader = ThumbnailImageLoaders.create(context)
}
