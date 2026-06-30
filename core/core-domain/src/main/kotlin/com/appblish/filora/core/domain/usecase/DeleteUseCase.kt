package com.appblish.filora.core.domain.usecase

import com.appblish.filora.core.common.result.OperationError
import com.appblish.filora.core.common.result.Result
import com.appblish.filora.core.common.result.asError
import com.appblish.filora.core.domain.model.DeleteOutcome
import com.appblish.filora.core.domain.repository.FileRepository
import javax.inject.Inject

/**
 * Deletes one or many files/folders (FR-3.4). The destructive confirmation is the
 * UI's responsibility (see the browser's delete dialog); by the time this runs the
 * user has confirmed. The use case normalises the request — dropping blanks and
 * de-duplicating paths — then delegates to the repository, which moves items to a
 * recoverable trash when [toTrash] is set and the platform supports it.
 *
 * An empty target list (after normalisation) is rejected with
 * [OperationError.NotFound] rather than reported as a vacuous success, so a
 * mis-wired caller fails loudly instead of silently doing nothing.
 */
class DeleteUseCase
    @Inject
    constructor(
        private val fileRepository: FileRepository,
    ) {
        suspend operator fun invoke(
            paths: List<String>,
            toTrash: Boolean = true,
        ): Result<DeleteOutcome> {
            val targets = paths.filter { it.isNotBlank() }.distinct()
            return if (targets.isEmpty()) {
                OperationError.NotFound().asError()
            } else {
                fileRepository.delete(targets, toTrash)
            }
        }
    }
