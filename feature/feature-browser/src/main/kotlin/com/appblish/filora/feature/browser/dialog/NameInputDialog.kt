package com.appblish.filora.feature.browser.dialog

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import com.appblish.filora.core.domain.usecase.FileNameValidation
import com.appblish.filora.core.domain.usecase.FileNameValidator
import com.appblish.filora.feature.browser.R

/**
 * Reusable single-field name dialog shared by the create-folder and rename flows.
 * Runs [FileNameValidator] live so the confirm button stays disabled and an inline
 * error shows while the typed name is invalid (FR-3.1 / FR-3.2). A post-submit
 * [submitError] (e.g. a conflict the data layer reported) is shown until the user
 * edits the text again.
 *
 * On confirm the trimmed, validated name is handed to [onConfirm]; the caller is
 * responsible for invoking the matching use case.
 */
@Suppress("SpreadOperator") // pluralStringResource requires vararg format args from the validator.
@Composable
internal fun NameInputDialog(
    title: String,
    confirmLabel: String,
    initialName: String,
    existingNames: Set<String>,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    submitError: String? = null,
) {
    var field by remember {
        mutableStateOf(
            TextFieldValue(text = initialName, selection = TextRange(0, initialName.length)),
        )
    }
    var edited by remember { mutableStateOf(false) }

    val validation = FileNameValidator.validate(field.text, existingNames)
    val validationMessage =
        (validation as? FileNameValidation.Invalid)
            ?.takeIf { edited }
            ?.error
            ?.toMessage()
    val validationError =
        if (validationMessage != null) {
            stringResource(validationMessage.resId, *validationMessage.formatArgs.toTypedArray())
        } else {
            null
        }
    // A fresh edit clears the stale server-side error; otherwise show it.
    val errorText = validationError ?: submitError?.takeUnless { edited }
    val confirmEnabled = validation is FileNameValidation.Valid

    val confirm: () -> Unit = {
        val valid = validation as? FileNameValidation.Valid
        if (valid != null) onConfirm(valid.name)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = field,
                onValueChange = {
                    field = it
                    edited = true
                },
                singleLine = true,
                isError = errorText != null,
                supportingText = errorText?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (confirmEnabled) confirm() }),
            )
        },
        confirmButton = {
            TextButton(onClick = confirm, enabled = confirmEnabled) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.browser_dialog_cancel)) }
        },
    )
}
