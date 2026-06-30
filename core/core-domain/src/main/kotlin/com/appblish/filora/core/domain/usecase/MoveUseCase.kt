package com.appblish.filora.core.domain.usecase

import com.appblish.filora.core.common.result.OperationError
import com.appblish.filora.core.common.result.Result
import com.appblish.filora.core.common.result.asSuccess
import com.appblish.filora.core.domain.model.ConflictStrategy
import com.appblish.filora.core.domain.model.FileItem
import com.appblish.filora.core.domain.model.TransferOutcome
import com.appblish.filora.core.domain.model.TransferResult
import com.appblish.filora.core.domain.repository.FileRepository
import javax.inject.Inject

/**
 * Moves [sources] into [destinationDir] as copy-verify-delete (FR-3.3): each
 * source is copied via [CopyUseCase] (so the same skip / replace / keep-both
 * conflict handling applies), the copy is verified, and only then is the source
 * deleted. A source is deleted permanently — never trashed — because its data
 * already lives at the destination.
 *
 * The ordering is the safety guarantee: if verification fails the source is left
 * untouched (no data loss), reported as [TransferOutcome.Failed]; if the delete
 * fails the source simply remains alongside the copy, again surfaced as a
 * per-item failure. Skipped and already-failed copies leave their source intact.
 */
class MoveUseCase
    @Inject
    constructor(
        private val copyUseCase: CopyUseCase,
        private val fileRepository: FileRepository,
    ) {
        suspend operator fun invoke(
            sources: List<FileItem>,
            destinationDir: String,
            strategy: ConflictStrategy,
        ): Result<List<TransferResult>> {
            val copyResults =
                when (val copied = copyUseCase(sources, destinationDir, strategy)) {
                    is Result.Success -> copied.data
                    is Result.Error -> return copied
                }

            val moved =
                copyResults.map { result ->
                    val transferred = result.outcome
                    if (transferred !is TransferOutcome.Transferred) {
                        result
                    } else {
                        result.copy(outcome = finalizeMove(result.source, transferred.destination))
                    }
                }
            return moved.asSuccess()
        }

        /** Verifies the copy landed, then deletes the source permanently. */
        private suspend fun finalizeMove(
            source: FileItem,
            destination: FileItem,
        ): TransferOutcome {
            if (!verified(source, destination)) {
                return TransferOutcome.Failed(OperationError.Io())
            }
            return when (val deleted = fileRepository.delete(listOf(source.path), toTrash = false)) {
                is Result.Success -> TransferOutcome.Transferred(destination)
                is Result.Error -> TransferOutcome.Failed(deleted.error)
            }
        }

        /**
         * Confirms the copy exists at the destination and, for files, matches the
         * source size. Directory trees are verified by existence alone here — a
         * deep content comparison belongs to the data layer's copy implementation.
         */
        private suspend fun verified(
            source: FileItem,
            destination: FileItem,
        ): Boolean =
            when (val check = fileRepository.getFile(destination.path)) {
                is Result.Success -> source.isDirectory || check.data.sizeBytes == source.sizeBytes
                is Result.Error -> false
            }
    }
