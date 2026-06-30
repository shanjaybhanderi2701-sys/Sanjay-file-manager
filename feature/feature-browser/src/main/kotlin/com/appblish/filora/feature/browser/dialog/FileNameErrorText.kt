package com.appblish.filora.feature.browser.dialog

import com.appblish.filora.core.domain.usecase.FileNameError

/**
 * Maps a [FileNameError] to a short, user-facing inline message. Kept beside the
 * dialogs (the only consumers) and using literal strings to match the existing
 * `core-ui` component style; switch to string resources when localization lands.
 */
internal fun FileNameError.toMessage(): String =
    when (this) {
        FileNameError.Blank -> "Enter a name"
        FileNameError.Reserved -> "\".\" and \"..\" are reserved names"
        FileNameError.TooLong -> "Name is too long (max 255 characters)"
        FileNameError.Duplicate -> "An item with this name already exists"
        is FileNameError.IllegalCharacters ->
            "Can't contain: " + characters.joinToString(separator = " ") { it.toString() }
    }
