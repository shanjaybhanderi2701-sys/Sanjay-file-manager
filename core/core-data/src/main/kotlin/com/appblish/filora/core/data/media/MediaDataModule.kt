package com.appblish.filora.core.data.media

import com.appblish.filora.core.domain.repository.MediaRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Wires the media-browsing data slice (T1.4) into the graph: the MediaStore read
 * source and the repository over it. Kept as `@Binds` interfaces so consumers
 * depend on the [MediaRepository]/[MediaStoreSource] contracts, not the
 * `ContentResolver` plumbing behind [AndroidMediaStoreSource].
 */
@Module
@InstallIn(SingletonComponent::class)
internal interface MediaDataModule {
    @Binds
    @Singleton
    fun bindMediaStoreSource(impl: AndroidMediaStoreSource): MediaStoreSource

    @Binds
    @Singleton
    fun bindMediaRepository(impl: MediaRepositoryImpl): MediaRepository
}
