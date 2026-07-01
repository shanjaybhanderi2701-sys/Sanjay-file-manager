package com.appblish.filora.core.domain.usecase

import com.appblish.filora.core.common.result.OperationError
import com.appblish.filora.core.common.result.Result
import com.appblish.filora.core.common.result.asError
import com.appblish.filora.core.common.result.map
import com.appblish.filora.core.domain.model.DeleteOutcome
import com.appblish.filora.core.domain.repository.FileRepository
import com.appblish.filora.core.domain.repository.TrashRepository
import javax.inject.Inject

/**
 * Deletes one or many files/folders (FR-3.4). The destructive confirmation is the
 * UI's responsibility (see the browser's delete dialog); by the time this runs the
 * user has confirmed. The use case normalises the request — dropping blanks and
 * de-duplicating paths — then routes it.
 *
 * When [toTrash] is set and **every** target is trashable (a local path Filora can
 * move — see [TrashRepository.canTrash]), the items are moved to the app-managed
 * recycle bin and the outcome reports `movedToTrash = true` so the UI can offer Undo
 * / point at the bin (NFR-2.4). Otherwise (permanent delete requested, or any target
 * is a `content://` SAF/MediaStore item that can't be reliably trashed) the whole
 * batch is removed permanently via the [FileRepository]. Keeping the batch atomic —
 * all-to-trash or all-permanent — avoids a confusing half-recoverable delete.
 *
 * An empty target list (after normalisation) is rejected with
 * [OperationError.NotFound] rather than reported as a vacuous success, so a
 * mis-wired caller fails loudly instead of silently doing nothing.
 */
class DeleteUseCase
    @Inject
    constructor(
        private val fileRepository: FileRepository,
        private val trashRepository: TrashRepository,
    ) {
        suspend operator fun invoke(
            paths: List<String>,
            toTrash: Boolean = true,
        ): Result<DeleteOutcome> {
            val targets = paths.filter { it.isNotBlank() }.distinct()
            if (targets.isEmpty()) return OperationError.NotFound().asError()

            val useTrash = toTrash && targets.all(trashRepository::canTrash)
            return if (useTrash) {
                trashRepository
                    .moveToTrash(targets)
                    .map { movedCount -> DeleteOutcome(deletedCount = movedCount, movedToTrash = true) }
            } else {
                fileRepository.delete(targets, toTrash = false)
            }
        }
    }
