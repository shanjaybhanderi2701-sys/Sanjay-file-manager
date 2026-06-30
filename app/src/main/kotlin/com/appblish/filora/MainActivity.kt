package com.appblish.filora

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.appblish.filora.core.ui.theme.FiloraTheme
import com.appblish.filora.navigation.FiloraNavHost
import com.appblish.filora.navigation.Route
import com.appblish.filora.permission.StoragePermissions
import dagger.hilt.android.AndroidEntryPoint

/** Single activity hosting the Compose navigation graph. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Gate the first run behind the permission rationale (FR-1.1); once media
        // access exists we launch straight into Home. Evaluated once per launch —
        // re-grants/revokes mid-session are handled by the surfaces that read media.
        val startDestination =
            if (StoragePermissions.hasMediaAccess(this)) Route.Home else Route.Permission

        setContent {
            FiloraTheme {
                FiloraNavHost(startDestination = startDestination)
            }
        }
    }
}
