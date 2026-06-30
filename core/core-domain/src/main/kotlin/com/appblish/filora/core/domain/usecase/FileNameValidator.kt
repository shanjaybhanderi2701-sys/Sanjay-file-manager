package com.appblish.filora.core.domain.usecase

/**
 * Pure validation for a new file/folder name, independent of any platform. Used
 * both by the create-folder / rename dialogs for live inline feedback (FR-3.1,
 * FR-3.2) and by the use cases as the final gate before touching the repository.
 *
 * Rules target the lowest common denominator across Filora's storage backends —
 * FAT/exFAT external volumes are the strictest, so their illegal-character set is
 * enforced everywhere to keep behaviour consistent regardless of where the item
 * lives. Spaces are allowed inside a name; only surrounding whitespace is trimmed.
 */
object FileNameValidator {
    /** Characters disallowed on FAT/exFAT, plus the path separators and NUL. */
    val ILLEGAL_CHARACTERS: Set<Char> =
        setOf('/', '\\', ':', '*', '?', '"', '<', '>', '|', '\u0000')

    /** Conservative cap; most filesystems allow up to 255 bytes for one name. */
    const val MAX_LENGTH: Int = 255

    /**
     * Validates [rawName], trimming surrounding whitespace first. When
     * [existingNames] is supplied (the sibling names in the target directory),
     * a case-insensitive match is rejected as a duplicate — this is what drives
     * the "duplicate name rejected with inline error" acceptance criterion.
     */
    fun validate(
        rawName: String,
        existingNames: Set<String> = emptySet(),
    ): FileNameValidation {
        val name = rawName.trim()
        val illegal: Set<Char> = name.toCharArray().filterTo(linkedSetOf()) { it in ILLEGAL_CHARACTERS }
        return when {
            name.isEmpty() -> FileNameValidation.Invalid(FileNameError.Blank)
            name == "." || name == ".." -> FileNameValidation.Invalid(FileNameError.Reserved)
            illegal.isNotEmpty() -> FileNameValidation.Invalid(FileNameError.IllegalCharacters(illegal))
            name.length > MAX_LENGTH -> FileNameValidation.Invalid(FileNameError.TooLong)
            existingNames.any { it.equals(name, ignoreCase = true) } ->
                FileNameValidation.Invalid(FileNameError.Duplicate)
            else -> FileNameValidation.Valid(name)
        }
    }
}

/** Outcome of [FileNameValidator.validate]. */
sealed interface FileNameValidation {
    /** The trimmed, ready-to-use [name]. */
    data class Valid(
        val name: String
    ) : FileNameValidation

    data class Invalid(
        val error: FileNameError
    ) : FileNameValidation
}

/** Why a name was rejected, for mapping to inline UI messages. */
sealed interface FileNameError {
    /** Empty or whitespace-only. */
    data object Blank : FileNameError

    /** "." or ".." — reserved directory references. */
    data object Reserved : FileNameError

    /** Longer than [FileNameValidator.MAX_LENGTH]. */
    data object TooLong : FileNameError

    /** A sibling with the same name (case-insensitive) already exists. */
    data object Duplicate : FileNameError

    /** Contains one or more characters from [FileNameValidator.ILLEGAL_CHARACTERS]. */
    data class IllegalCharacters(
        val characters: Set<Char>
    ) : FileNameError
}
