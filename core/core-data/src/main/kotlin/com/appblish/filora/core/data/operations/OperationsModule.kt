package com.appblish.filora.core.data.operations

import android.content.Context
import androidx.work.WorkManager
import com.appblish.filora.core.domain.repository.FileOperationsScheduler
import com.appblish.filora.core.domain.repository.FileRepository
import dagger.Binds
import dagger.BindsOptionalOf
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.UUID
import javax.inject.Singleton

/**
 * Wires the background file-operation layer (FR-3.5): the [WorkManager] instance
 * the [OperationScheduler] enqueues into, and the id generator it stamps unique
 * work with. [WorkRequestStore] and [OperationScheduler] are `@Inject`-annotated,
 * so Hilt constructs them without explicit provider methods.
 */
@Module
@InstallIn(SingletonComponent::class)
object OperationsModule {
    @Provides
    @Singleton
    fun provideWorkManager(
        @ApplicationContext context: Context,
    ): WorkManager = WorkManager.getInstance(context)

    @Provides
    fun provideOperationIdGenerator(): OperationIdGenerator = OperationIdGenerator { "filora-op-" + UUID.randomUUID() }
}

/**
 * Declares [FileRepository] as an *optional* dependency of the operation workers.
 * The concrete data binding is contributed by the data-layer file-operations slice
 * and is not wired yet; `@BindsOptionalOf` lets `:app` assemble its Hilt graph
 * today (resolving to an empty `Optional`) and automatically pick up the real
 * binding once it lands. The use cases are built directly from the resolved
 * repository inside the worker, so they are deliberately *not* requested from Hilt
 * (that would pull the still-missing `FileRepository` binding into the graph).
 */
@Module
@InstallIn(SingletonComponent::class)
internal interface FileOperationsBindings {
    @BindsOptionalOf
    fun optionalFileRepository(): FileRepository

    /**
     * Exposes the WorkManager-backed [OperationScheduler] under the [FileOperationsScheduler]
     * domain seam so feature modules (which see `core-domain` only) can enqueue and observe
     * copy/move/zip operations without depending on `core-data`.
     */
    @Binds
    fun bindFileOperationsScheduler(impl: OperationScheduler): FileOperationsScheduler
}
