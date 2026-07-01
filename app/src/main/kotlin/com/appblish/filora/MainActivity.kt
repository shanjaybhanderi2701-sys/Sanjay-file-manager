package com.appblish.filora

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appblish.filora.core.data.storage.SafTreeAccess
import com.appblish.filora.core.domain.model.ThemeMode
import com.appblish.filora.core.domain.model.UserPreferences
import com.appblish.filora.core.domain.repository.SettingsRepository
import com.appblish.filora.core.ui.theme.FiloraTheme
import com.appblish.filora.navigation.FiloraApp
import com.appblish.filora.navigation.FiloraDeepLinks
import com.appblish.filora.navigation.Route
import com.appblish.filora.navigation.ViewIntentValidator
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

    @OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // B3 / security-impl-audit F1: neutralise a hostile exported deep link before
        // Navigation consumes the launch intent (see [sanitizeInboundDeepLink]).
        sanitizeInboundDeepLink(intent)

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
                // Expose Compose `testTag`s as resource-ids so UiAutomator (baseline
                // profile generator + macrobenchmarks, T7.4 / NFR-1.1) can address nodes
                // via `By.res(packageName, tag)`. No effect on production behavior.
                FiloraApp(
                    startDestination = startDestination,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("filora_app_root")
                        .semantics { testTagsAsResourceId = true },
                )
            }
        }
    }

    // A running app is re-delivered VIEW intents here; sanitise the same way and pin the
    // cleaned intent back so any downstream deep-link handling sees the safe version.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        sanitizeInboundDeepLink(intent)
        setIntent(intent)
    }

    /**
     * B3 / security-impl-audit F1. The `filora://` `VIEW` filter is exported + BROWSABLE,
     * so any web page can fire a deep link. Before Navigation consumes it we run
     * [ViewIntentValidator]; a link pointing outside public shared storage / a held SAF
     * grant (e.g. `filora://browser?location=/data/data/…`, an un-granted `content://`, a
     * `..` traversal, or an unknown category) is neutralised to a plain `MAIN` launch, so
     * the graph falls back to its start destination (Home) instead of browsing an
     * attacker-chosen location. Never crashes: a malformed link is simply dropped.
     *
     * The [Route] args are optional, so Navigation routes on the **query** form
     * (`filora://browser?location=…`). But a hostile link can instead put the target in the
     * URI **path** (`filora://browser/data/data/…`); to keep validation independent of how
     * Navigation happens to resolve the template, we validate the query value when present
     * and otherwise fall back to the decoded path tail — whichever a link actually carries.
     */
    private fun sanitizeInboundDeepLink(intent: Intent?) {
        if (intent == null || intent.action != Intent.ACTION_VIEW) return
        val data = intent.data ?: return
        if (!FiloraDeepLinks.SCHEME.equals(data.scheme, ignoreCase = true)) return

        val validator = ViewIntentValidator(
            grantedTreeUris = { safTreeAccess.persistedTreeUris().mapTo(HashSet<String>()) { it.toString() } },
        )
        val pathCandidate = pathTail(data)
        val allowed = runCatching {
            validator.isDeepLinkAllowed(
                host = data.host,
                locationArg = data.getQueryParameter("location") ?: pathCandidate,
                categoryArg = data.getQueryParameter("category") ?: pathCandidate,
            )
        }.getOrDefault(false)

        if (!allowed) {
            intent.action = Intent.ACTION_MAIN
            intent.data = null
        }
    }

    /**
     * The decoded URI path after the host, re-joined with `/`, or `null` when the link has no
     * path (the plain query form). E.g. `filora://browser/data/data/app/db` → `data/data/app/db`;
     * `filora://browser?location=…` → `null`. [Uri.getPathSegments] is already percent-decoded and
     * drops empty segments, so a leading-slash or double-slash link cannot smuggle a segment past us.
     */
    private fun pathTail(data: Uri): String? =
        data.pathSegments.takeIf { it.isNotEmpty() }?.joinToString("/")
}
