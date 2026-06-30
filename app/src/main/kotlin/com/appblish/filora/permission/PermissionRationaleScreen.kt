package com.appblish.filora.permission

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.appblish.filora.R

/**
 * First-run permission rationale (FR-1.1). Explains *why* Filora needs media
 * access before the system dialog appears, then requests the level-appropriate
 * read permissions.
 *
 * - **Grant** → [onGranted] (proceeds to Home).
 * - **Deny / "continue with limited access"** → launches the SAF tree picker
 *   ([ActivityResultContracts.OpenDocumentTree], T1.3). SAF needs no runtime
 *   permission; the chosen tree is persisted via [PermissionViewModel] so the
 *   grant survives restart, and then [onContinueWithLimitedAccess] proceeds to
 *   Home. Backing out of the picker leaves the user on the gate to retry or grant
 *   media access instead — never a dead end.
 * - **Permanently denied** (T028) → once the OS stops showing the system dialog,
 *   the denied state offers an "Open settings" deep link
 *   ([AppSettings.appDetailsIntent]) so a grant is still reachable — never a dead end.
 * - **All-files access** (T022) → only on the gated `fullaccess` build
 *   ([AllFilesAccess.shouldOffer]); shows an explicit justification dialog before
 *   routing to the system all-files toggle.
 *
 * This stateful wrapper owns the request UX (launchers + denied/justification
 * state); the granted/denied decision is surfaced to the caller through the two
 * callbacks so navigation stays in the host graph. Layout lives in the stateless
 * [PermissionRationaleContent].
 */
@Composable
fun PermissionRationaleScreen(
    onGranted: () -> Unit,
    onContinueWithLimitedAccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PermissionViewModel = hiltViewModel(),
) {
    var denied by rememberSaveable { mutableStateOf(false) }
    var showFullAccessJustification by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    val launcher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) { results ->
            // Any granted read permission (incl. a partial photo grant) is enough
            // to unlock the media-backed surfaces; otherwise fall back to SAF.
            if (results.values.any { it }) {
                onGranted()
            } else {
                denied = true
            }
        }

    // SAF tree picker (T1.3): the user grants a directory; we take a persistable
    // grant on it so it outlives this process, then proceed. A null result means
    // the picker was dismissed — stay on the gate so the choice can be made again.
    val treePicker =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocumentTree(),
        ) { treeUri ->
            if (treeUri != null) {
                viewModel.onTreeGranted(treeUri)
                onContinueWithLimitedAccess()
            }
        }

    PermissionRationaleContent(
        denied = denied,
        showFullAccessOptIn = AllFilesAccess.shouldOffer(),
        onGrantClick = {
            launcher.launch(StoragePermissions.requiredReadPermissions().toTypedArray())
        },
        onLimitedClick = { treePicker.launch(null) },
        onOpenSettingsClick = {
            context.startActivity(AppSettings.appDetailsIntent(context.packageName))
        },
        onFullAccessClick = { showFullAccessJustification = true },
        modifier = modifier,
    )

    if (showFullAccessJustification) {
        FullAccessJustificationDialog(
            onConfirm = {
                showFullAccessJustification = false
                context.startActivity(AllFilesAccess.manageAccessIntent(context.packageName))
            },
            onDismiss = { showFullAccessJustification = false },
        )
    }
}

/** Stateless rationale layout; all decisions are routed through the callbacks. */
@Composable
private fun PermissionRationaleContent(
    denied: Boolean,
    showFullAccessOptIn: Boolean,
    onGrantClick: () -> Unit,
    onLimitedClick: () -> Unit,
    onOpenSettingsClick: () -> Unit,
    onFullAccessClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp, vertical = 32.dp)
                    .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.FolderOpen,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(72.dp),
            )

            Text(
                text = stringResource(R.string.permission_title),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 24.dp),
            )

            Text(
                text =
                    stringResource(
                        if (denied) R.string.permission_denied_body else R.string.permission_rationale_body,
                    ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp),
            )

            if (!denied) {
                Button(
                    onClick = onGrantClick,
                    modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                ) {
                    Text(stringResource(R.string.permission_grant_button))
                }
            }

            PermissionSecondaryActions(
                denied = denied,
                showFullAccessOptIn = showFullAccessOptIn,
                onLimitedClick = onLimitedClick,
                onOpenSettingsClick = onOpenSettingsClick,
                onFullAccessClick = onFullAccessClick,
            )
        }
    }
}

/**
 * The always-available SAF path plus the conditional escape hatches: the
 * permanent-denial settings deep link (T028) and the gated all-files opt-in (T022).
 */
@Composable
private fun PermissionSecondaryActions(
    denied: Boolean,
    showFullAccessOptIn: Boolean,
    onLimitedClick: () -> Unit,
    onOpenSettingsClick: () -> Unit,
    onFullAccessClick: () -> Unit,
) {
    TextButton(
        // Launch the system document-tree picker; `null` opens at the picker's default root.
        onClick = onLimitedClick,
        modifier = Modifier.fillMaxWidth().padding(top = if (denied) 32.dp else 8.dp),
    ) {
        Text(stringResource(R.string.permission_limited_button))
    }

    // T028: after a denial the system dialog may no longer appear ("Don't ask
    // again"), so the only path to a grant is App info — offer it as an escape hatch.
    if (denied) {
        TextButton(
            onClick = onOpenSettingsClick,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) {
            Text(stringResource(R.string.permission_open_settings_button))
        }
    }

    // T022: all-files access opt-in — gated to the `fullaccess` build only, and
    // always behind an explicit justification dialog.
    if (showFullAccessOptIn) {
        TextButton(
            onClick = onFullAccessClick,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) {
            Text(stringResource(R.string.permission_full_access_button))
        }
    }
}
