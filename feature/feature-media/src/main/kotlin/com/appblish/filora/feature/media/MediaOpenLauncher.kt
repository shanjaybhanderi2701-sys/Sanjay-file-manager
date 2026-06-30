package com.appblish.filora.feature.media

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.appblish.filora.core.domain.model.FileItem
import com.appblish.filora.core.domain.usecase.ShareIntentPlanner

/**
 * Opens a media item picked from a category hub in the app the system associates
 * with its type, via `ACTION_VIEW` (FR-6.1).
 *
 * MediaStore entries are already addressed by `content://` URIs (see
 * `MediaRepositoryImpl`/`AndroidMediaStoreSource`), so — unlike the browser's
 * filesystem launcher — no FileProvider staging is needed: the URI is forwarded
 * directly under a scoped, time-bounded read grant (NFR-3.3). The MIME type is
 * negotiated by the platform-free [ShareIntentPlanner] so the chooser surfaces the
 * right handlers (image viewer, video/audio player, document app…).
 *
 * Every outcome is non-fatal: a directory, a non-content locator, or a device with
 * no matching handler all return `false` so the screen can show a "can't open"
 * message instead of crashing (FR-6.1 AC).
 */
object MediaOpenLauncher {
    private const val CONTENT_SCHEME = "content://"

    /**
     * Launches a viewer for [item]. Returns `false` (and dispatches nothing) when the
     * item is not openable, is not a content URI, or no installed app can handle it.
     */
    fun open(
        context: Context,
        item: FileItem,
    ): Boolean {
        val plan = ShareIntentPlanner.planOpen(item)
        if (!plan.isOpenable || !item.path.startsWith(CONTENT_SCHEME)) return false

        val uri = Uri.parse(item.path)
        val intent =
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, plan.mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                // ClipData carries the grant to the chosen handler for this URI only.
                clipData = ClipData.newRawUri(item.name, uri)
            }
        return dispatch(context, intent)
    }

    private fun dispatch(
        context: Context,
        intent: Intent,
    ): Boolean {
        val chooser =
            Intent.createChooser(intent, "Open with").apply {
                // The launching context may be the application context, not an Activity.
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        return try {
            context.startActivity(chooser)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }
}
