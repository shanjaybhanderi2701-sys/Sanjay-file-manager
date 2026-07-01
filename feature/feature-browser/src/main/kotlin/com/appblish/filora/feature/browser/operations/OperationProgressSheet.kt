package com.appblish.filora.feature.browser.operations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.appblish.filora.feature.browser.R

/**
 * Live worker-progress surface for an in-flight copy/move/zip (FR-3.5, T079). Shows a
 * determinate bar when the fraction is known and an indeterminate one while pending or
 * of unknown size, the item currently being processed, and a cancel control (T074).
 * Terminal states are reported by a snackbar, so reaching one dismisses this sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperationProgressSheet(
    operation: ActiveOperation,
    onCancel: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        // Dismissing the sheet cancels the operation, matching the explicit cancel button.
        onDismissRequest = onCancel,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(titleRes(operation.kind)),
                style = MaterialTheme.typography.titleMedium,
            )
            val fraction = operation.fraction
            if (fraction != null) {
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            operation.currentName?.let { name ->
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TextButton(
                onClick = onCancel,
                modifier = Modifier.align(Alignment.End).padding(bottom = 16.dp),
            ) {
                Text(stringResource(R.string.browser_op_cancel))
            }
        }
    }
}

private fun titleRes(kind: BatchOperationKind): Int =
    when (kind) {
        BatchOperationKind.COPY -> R.string.browser_op_title_copy
        BatchOperationKind.MOVE -> R.string.browser_op_title_move
        BatchOperationKind.ZIP -> R.string.browser_op_title_zip
    }
