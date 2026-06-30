package com.appblish.filora.core.domain.usecase

import com.appblish.filora.core.common.result.OperationError
import com.appblish.filora.core.common.result.Result
import com.appblish.filora.core.common.result.asError
import com.appblish.filora.core.domain.model.FileItem
import com.appblish.filora.core.domain.repository.FileRepository
import javax.inject.Inject

/**
 * Creates a new folder named [rawName] under [parentPath] (FR-3.1).
 *
 * The name is validated and trimmed first; an invalid name short-circuits to
 * [OperationError.InvalidName] without touching the repository. A duplicate name
 * surfaces as [OperationError.Conflict] from the data layer (the inline-error
 * acceptance criterion) — the UI also runs [FileNameValidator] against the
 * sibling names for live feedback before the user ever submits.
 */
class CreateFolderUseCase
    @Inject
    constructor(
        private val fileRepository: FileRepository,
    ) {
        suspend operator fun invoke(
            parentPath: String,
            rawName: String,
        ): Result<FileItem> =
            when (val validation = FileNameValidator.validate(rawName)) {
                is FileNameValidation.Invalid -> OperationError.InvalidName().asError()
                is FileNameValidation.Valid -> fileRepository.createFolder(parentPath, validation.name)
            }
    }
