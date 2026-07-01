@file:Suppress("MatchingDeclarationName") // File name reflects the UI role; sole declaration is FileNameErrorMessage.

package com.appblish.filora.feature.browser.dialog

import androidx.annotation.StringRes
import com.appblish.filora.core.domain.usecase.FileNameError
import com.appblish.filora.feature.browser.R

/**
 * A resolved, user-facing message descriptor for a [FileNameError]: the string
 * resource plus any runtime format arguments. Kept free of Compose/Android types so
 * the mapping stays pure and the wording lives in resources (NFR-7); the dialog
 * resolves [resId]/[formatArgs] via `stringResource` at the call site.
 */
internal data class FileNameErrorMessage(
    @StringRes val resId: Int,
    val formatArgs: List<Any>,
)

/** Maps a [FileNameError] to the string resource and arguments used to render it. */
internal fun FileNameError.toMessage(): FileNameErrorMessage =
    when (this) {
        FileNameError.Blank -> FileNameErrorMessage(R.string.browser_name_error_blank, emptyList())
        FileNameError.Reserved -> FileNameErrorMessage(R.string.browser_name_error_reserved, emptyList())
        FileNameError.TooLong -> FileNameErrorMessage(R.string.browser_name_error_too_long, emptyList())
        FileNameError.Duplicate -> FileNameErrorMessage(R.string.browser_name_error_duplicate, emptyList())
        is FileNameError.IllegalCharacters ->
            FileNameErrorMessage(
                R.string.browser_name_error_illegal_characters,
                listOf(characters.joinToString(separator = " ") { it.toString() }),
            )
    }
