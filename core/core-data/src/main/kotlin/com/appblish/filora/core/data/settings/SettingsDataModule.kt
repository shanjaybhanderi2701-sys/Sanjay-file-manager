package com.appblish.filora.core.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.appblish.filora.core.common.dispatcher.IoDispatcher
import com.appblish.filora.core.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

/**
 * Wires the settings slice (T7.1). The single Preferences [DataStore] is created
 * once for the process and bound to a [SettingsRepository] so consumers depend on
 * the contract, not the on-disk file. Disk IO runs on the injected
 * [IoDispatcher] (provided in `:app`), keeping the read/write off the main thread.
 *
 * Both the [DataStore] provider and the repository binding are `@Singleton` in the
 * [SingletonComponent], so in production exactly one DataStore is ever active on the
 * `filora_settings` file. This module is intentionally public (not `internal`) so a
 * Hilt instrumentation test can `@TestInstallIn(replaces = [SettingsDataModule::class])`
 * to swap in a per-test file — `HiltAndroidRule` rebuilds the singleton graph per test,
 * which would otherwise open a second DataStore on the same file (see APP-149).
 */
@Module
@InstallIn(SingletonComponent::class)
interface SettingsDataModule {
    @Binds
    @Singleton
    fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    companion object {
        private const val PREFERENCES_NAME = "filora_settings"

        @Provides
        @Singleton
        fun providePreferencesDataStore(
            @ApplicationContext context: Context,
            @IoDispatcher ioDispatcher: CoroutineDispatcher,
        ): DataStore<Preferences> =
            PreferenceDataStoreFactory.create(
                scope = CoroutineScope(ioDispatcher + SupervisorJob()),
                produceFile = { context.preferencesDataStoreFile(PREFERENCES_NAME) },
            )
    }
}
