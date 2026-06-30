package com.appblish.filora.core.data.search

import com.appblish.filora.core.domain.repository.SearchRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds the streaming search slice (T5.1) into the graph so consumers depend on the
 * [SearchRepository] contract, not the java.io/SAF walk behind [FileSystemSearchRepository].
 */
@Module
@InstallIn(SingletonComponent::class)
internal interface SearchDataModule {
    @Binds
    @Singleton
    fun bindSearchRepository(impl: FileSystemSearchRepository): SearchRepository
}
