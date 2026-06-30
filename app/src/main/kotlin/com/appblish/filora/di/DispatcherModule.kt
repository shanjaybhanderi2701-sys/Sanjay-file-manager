package com.appblish.filora.di

import com.appblish.filora.core.common.dispatcher.DefaultDispatcher
import com.appblish.filora.core.common.dispatcher.IoDispatcher
import com.appblish.filora.core.common.dispatcher.MainDispatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Binds the coroutine dispatcher qualifiers declared in `core-common`. Lives in
 * `:app` (the DI assembly point) so `core-common` stays Hilt-free.
 */
@Module
@InstallIn(SingletonComponent::class)
object DispatcherModule {
    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Provides
    @MainDispatcher
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main
}
