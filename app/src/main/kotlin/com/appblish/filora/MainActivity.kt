package com.appblish.filora

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appblish.filora.core.data.storage.SafTreeAccess
import com.appblish.filora.core.domain.model.ThemeMode
import com.appblish.filora.core.domain.model.UserPreferences
import com.appblish.filora.core.domain.repository.SettingsRepository
import com.appblish.filora.core.ui.theme.FiloraTheme
import com.appblish.filora.navigation.FiloraNavHost
import com.appblish.filora.navigation.Route
import com.appblish.filora.permission.StoragePermissions
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Single activity hosting the Compose navigation graph. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var safTreeAccess: SafTreeAccess

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Gate the first run behind the permission rationale (FR-1.1); once media
        // access exists — or the user has already persisted a SAF tree grant that
        // survived a restart (T1.3) — we launch straight into Home. Evaluated once
        // per launch; re-grants/revokes mid-session are handled by the surfaces
        // that read media.
        val hasAccess =
            StoragePermissions.hasMediaAccess(this) || safTreeAccess.hasPersistedTree()
        val startDestination = if (hasAccess) Route.Home else Route.Permission

        setContent {
            // FR-11.1: the theme follows the persisted preference and re-composes
            // the instant the user changes it on the Settings screen, since both
            // read the same DataStore-backed flow.
            val preferences by settingsRepository.preferences
                .collectAsStateWithLifecycle(initialValue = UserPreferences.Default)
            val darkTheme =
                when (preferences.themeMode) {
                    ThemeMode.System -> isSystemInDarkTheme()
                    ThemeMode.Light -> false
                    ThemeMode.Dark -> true
                }

            FiloraTheme(
                darkTheme = darkTheme,
                dynamicColor = preferences.useDynamicColor,
            ) {
                FiloraNavHost(startDestination = startDestination)
            }
        }
    }
}
