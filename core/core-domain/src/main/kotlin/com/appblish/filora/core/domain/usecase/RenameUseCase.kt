package com.appblish.filora.core.domain.usecase

import com.appblish.filora.core.common.result.OperationError
import com.appblish.filora.core.common.result.Result
import com.appblish.filora.core.common.result.asError
import com.appblish.filora.core.domain.model.FileItem
import com.appblish.filora.core.domain.repository.FileRepository
import javax.inject.Inject

/**
 * Renames the file/folder at [path] to [rawName] (FR-3.2).
 *
 * Invalid characters are rejected up front with [OperationError.InvalidName]
 * (the acceptance criterion); the data layer returns the updated [FileItem] so
 * the caller can reflect the rename without a full directory reload. Renaming to
 * the same name (after trimming) is a no-op that returns the current item, which
 * the UI can resolve via [currentName] without a repository round-trip.
 */
class RenameUseCase
    @Inject
    constructor(
        private val fileRepository: FileRepository,
    ) {
        suspend operator fun invoke(
            path: String,
            rawName: String,
            currentName: String? = null,
        ): Result<FileItem> =
            when (val validation = FileNameValidator.validate(rawName)) {
                is FileNameValidation.Invalid -> OperationError.InvalidName().asError()
                is FileNameValidation.Valid ->
                    if (validation.name == currentName) {
                        fileRepository.getFile(path)
                    } else {
                        fileRepository.rename(path, validation.name)
                    }
            }
    }
