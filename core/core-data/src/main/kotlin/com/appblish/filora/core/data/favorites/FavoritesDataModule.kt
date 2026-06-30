package com.appblish.filora.core.data.favorites

import com.appblish.filora.core.domain.repository.FavoritesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Wires the favorites/recents slice (T6.2). The DAOs are provided by
 * `core-database`'s `DatabaseModule`; here we only bind the Room-backed
 * implementation to the domain [FavoritesRepository] contract so consumers depend on
 * the interface, not the database.
 */
@Module
@InstallIn(SingletonComponent::class)
internal interface FavoritesDataModule {
    @Binds
    @Singleton
    fun bindFavoritesRepository(impl: FavoritesRepositoryImpl): FavoritesRepository
}
