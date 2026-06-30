package com.appblish.filora.core.domain.usecase

import com.appblish.filora.core.common.result.Result
import com.appblish.filora.core.common.result.asSuccess
import com.appblish.filora.core.domain.model.ConflictStrategy
import com.appblish.filora.core.domain.model.FileItem
import com.appblish.filora.core.domain.model.TransferOutcome
import com.appblish.filora.core.domain.model.TransferResult
import com.appblish.filora.core.domain.repository.FileRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Copies [sources] into [destinationDir], resolving name collisions with the
 * chosen [ConflictStrategy] (FR-3.3 skip / replace / keep-both).
 *
 * The destination directory is listed once up front; collisions are resolved
 * per item against that listing, and a keep-both name generated for one item is
 * reserved so a second same-named source becomes " (2)" rather than colliding
 * again. The batch returns [Result.Error] only when the destination cannot be
 * read; per-item failures are reported as [TransferOutcome.Failed] inside the
 * returned [TransferResult] list so a partial copy is fully observable.
 */
class CopyUseCase
    @Inject
    constructor(
        private val fileRepository: FileRepository,
    ) {
        suspend operator fun invoke(
            sources: List<FileItem>,
            destinationDir: String,
            strategy: ConflictStrategy,
        ): Result<List<TransferResult>> {
            val existingNames =
                when (val listing = fileRepository.listDirectory(destinationDir).first()) {
                    is Result.Success -> listing.data.mapTo(mutableSetOf()) { it.name }
                    is Result.Error -> return listing
                }

            val results =
                sources.map { source ->
                    val outcome = transfer(source, destinationDir, existingNames, strategy)
                    TransferResult(source, outcome)
                }
            return results.asSuccess()
        }

        private suspend fun transfer(
            source: FileItem,
            destinationDir: String,
            existingNames: MutableSet<String>,
            strategy: ConflictStrategy,
        ): TransferOutcome =
            when (
                val resolution =
                    ConflictResolver.resolve(source.name, source.isDirectory, existingNames, strategy)
            ) {
                ConflictResolution.Skip -> TransferOutcome.Skipped
                is ConflictResolution.Proceed ->
                    when (
                        val copied =
                            fileRepository.copy(
                                sourcePath = source.path,
                                destinationDir = destinationDir,
                                destinationName = resolution.name,
                                overwrite = resolution.overwrite,
                            )
                    ) {
                        is Result.Success -> {
                            // Reserve the written name so later keep-both items step past it.
                            existingNames += copied.data.name
                            TransferOutcome.Transferred(copied.data)
                        }
                        is Result.Error -> TransferOutcome.Failed(copied.error)
                    }
            }
    }
