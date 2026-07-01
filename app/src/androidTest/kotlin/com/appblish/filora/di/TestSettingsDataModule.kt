package com.appblish.filora.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.appblish.filora.core.data.settings.SettingsDataModule
import com.appblish.filora.core.data.settings.SettingsRepositoryImpl
import com.appblish.filora.core.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Singleton

/**
 * APP-149: replaces the production [SettingsDataModule] in the instrumentation APK.
 *
 * The production module is correct — both the `DataStore<Preferences>` provider and the
 * repository binding are `@Singleton`, so a real app process only ever opens one DataStore
 * on `filora_settings`. The failure is test-only: [dagger.hilt.android.testing.HiltAndroidRule]
 * rebuilds the `@Singleton` graph for every test method/class in the shared instrumentation
 * process, and each rebuild would call `PreferenceDataStoreFactory.create(...)` on the same
 * `filora_settings` file while the previous store's `SupervisorJob` scope is still alive —
 * DataStore then throws `IllegalStateException: There are multiple DataStores active for the
 * same file`.
 *
 * We keep the exact production binding shape (both `@Singleton`) but point each freshly built
 * graph at its own uniquely named file under the app cache dir, so no two DataStores ever
 * contend for the same file. The permission/smoke/nav tests only read defaults, so an empty
 * per-test store is behaviourally identical to the real one for their assertions.
 */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [SettingsDataModule::class])
interface TestSettingsDataModule {
    @Binds
    @Singleton
    fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    companion object {
        /** Distinguishes the file per Hilt-graph rebuild without relying on wall-clock time. */
        private val fileCounter = AtomicInteger(0)

        @Provides
        @Singleton
        fun providePreferencesDataStore(
            @ApplicationContext context: Context,
        ): DataStore<Preferences> {
            val unique = "${fileCounter.incrementAndGet()}_${UUID.randomUUID()}"
            return PreferenceDataStoreFactory.create(
                scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
                produceFile = {
                    File(context.cacheDir, "test_filora_settings_$unique.preferences_pb")
                },
            )
        }
    }
}
