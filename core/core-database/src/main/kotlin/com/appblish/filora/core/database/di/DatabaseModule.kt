package com.appblish.filora.core.database.di

import android.content.Context
import androidx.room.Room
import com.appblish.filora.core.database.FiloraDatabase
import com.appblish.filora.core.database.dao.FavoriteDao
import com.appblish.filora.core.database.dao.RecentDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Provides the Room database and its DAOs to the application graph. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): FiloraDatabase =
        Room
            .databaseBuilder(context, FiloraDatabase::class.java, FiloraDatabase.NAME)
            .build()

    @Provides
    fun provideFavoriteDao(database: FiloraDatabase): FavoriteDao = database.favoriteDao()

    @Provides
    fun provideRecentDao(database: FiloraDatabase): RecentDao = database.recentDao()
}
