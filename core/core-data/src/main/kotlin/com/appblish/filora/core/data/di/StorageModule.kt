package com.appblish.filora.core.data.di

import android.content.Context
import android.os.storage.StorageManager
import androidx.core.content.getSystemService
import com.appblish.filora.core.data.storage.AndroidVolumeEnumerator
import com.appblish.filora.core.data.storage.StorageRepositoryImpl
import com.appblish.filora.core.data.storage.VolumeEnumerator
import com.appblish.filora.core.domain.repository.StorageRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Wires the storage data layer (volume enumeration + insights) into the graph. */
@Module
@InstallIn(SingletonComponent::class)
abstract class StorageModule {
    @Binds
    abstract fun bindStorageRepository(impl: StorageRepositoryImpl): StorageRepository

    @Binds
    abstract fun bindVolumeEnumerator(impl: AndroidVolumeEnumerator): VolumeEnumerator

    companion object {
        @Provides
        @Singleton
        fun provideStorageManager(
            @ApplicationContext context: Context,
        ): StorageManager = requireNotNull(context.getSystemService())
    }
}
