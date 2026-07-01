package com.appblish.filora.core.data.trash

import com.appblish.filora.core.domain.repository.TrashRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Wires the recycle-bin slice (M12). The [TrashDao] is provided by `core-database`'s
 * `DatabaseModule`; here we only bind the app-managed [AppTrashRepository] to the
 * domain [TrashRepository] contract so consumers depend on the interface, not the
 * on-disk trash directory.
 */
@Module
@InstallIn(SingletonComponent::class)
internal interface TrashDataModule {
    @Binds
    @Singleton
    fun bindTrashRepository(impl: AppTrashRepository): TrashRepository
}
