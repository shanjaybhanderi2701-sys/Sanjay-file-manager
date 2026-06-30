package com.appblish.filora.core.data.file

import com.appblish.filora.core.domain.repository.FileRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds the directory-browsing slice (T034) so consumers depend on the
 * [FileRepository] contract rather than the java.io/SAF backends behind
 * [FileRepositoryImpl]. This is the concrete binding the file-operations layer
 * declared `@BindsOptionalOf` for, so wiring it here also turns the operation
 * workers' optional repository present.
 */
@Module
@InstallIn(SingletonComponent::class)
internal interface FileDataModule {
    @Binds
    @Singleton
    fun bindFileRepository(impl: FileRepositoryImpl): FileRepository
}
