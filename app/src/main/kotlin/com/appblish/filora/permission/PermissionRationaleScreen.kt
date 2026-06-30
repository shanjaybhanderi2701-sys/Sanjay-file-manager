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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.appblish.filora.R

/**
 * First-run permission rationale (FR-1.1). Explains *why* Filora needs media
 * access before the system dialog appears, then requests the level-appropriate
 * read permissions.
 *
 * - **Grant** → [onGranted] (proceeds to Home).
 * - **Deny** (or "continue with limited access") → [onContinueWithLimitedAccess],
 *   the SAF-scoped browsing path. SAF needs no runtime permission, so the app
 *   stays usable; the tree picker itself is wired in T1.3.
 *
 * The screen owns only the request UX; the granted/denied decision is surfaced
 * to the caller through the two callbacks so navigation stays in the host graph.
 */
@Composable
fun PermissionRationaleScreen(
    onGranted: () -> Unit,
    onContinueWithLimitedAccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var denied by rememberSaveable { mutableStateOf(false) }

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
                        if (denied) {
                            R.string.permission_denied_body
                        } else {
                            R.string.permission_rationale_body
                        },
                    ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp),
            )

            if (!denied) {
                Button(
                    onClick = {
                        launcher.launch(
                            StoragePermissions.requiredReadPermissions().toTypedArray(),
                        )
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp),
                ) {
                    Text(stringResource(R.string.permission_grant_button))
                }
            }

            TextButton(
                onClick = onContinueWithLimitedAccess,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = if (denied) 32.dp else 8.dp),
            ) {
                Text(stringResource(R.string.permission_limited_button))
            }
        }
    }
}
