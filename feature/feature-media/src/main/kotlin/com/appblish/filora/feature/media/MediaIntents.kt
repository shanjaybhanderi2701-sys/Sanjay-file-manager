package com.appblish.filora.feature.media

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.appblish.filora.core.domain.model.FileItem
import com.appblish.filora.core.domain.usecase.ShareIntentPlanner

/**
 * Builds and dispatches open/play/share intents for items picked from a media
 * category hub (FR-6.1, FR-10).
 *
 * MediaStore entries are already addressed by `content://` URIs (see
 * `MediaRepositoryImpl`/`AndroidMediaStoreSource`), so — unlike the browser's
 * filesystem launcher — no FileProvider staging is needed here: the URI is forwarded
 * directly under a scoped, time-bounded read grant (NFR-3.3). MIME negotiation is
 * delegated to the platform-free [ShareIntentPlanner] so the chooser surfaces the
 * right handlers (image viewer, video/audio player, document app…).
 *
 * Every dispatch is wrapped in a chooser and guarded against
 * [ActivityNotFoundException]: a directory, a non-content locator, or a device with
 * no matching handler all return `false` so the screen can show a "can't open / can't
 * share" message instead of crashing (FR-6.1 / FR-10.1 AC).
 */
object MediaIntents {
    private const val CONTENT_SCHEME = "content://"

    /**
     * Launches a viewer/player for [item] via `ACTION_VIEW`. Returns `false` (and
     * dispatches nothing) when the item is not openable, is not a content URI, or no
     * installed app can handle it.
     */
    fun open(
        context: Context,
        item: FileItem,
    ): Boolean {
        if (!isOpenable(item)) return false

        val uri = Uri.parse(item.path)
        val intent =
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, ShareIntentPlanner.openType(item))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                // ClipData carries the grant to the chosen handler for this URI only.
                clipData = ClipData.newRawUri(item.name, uri)
            }
        return dispatch(context, intent, context.getString(R.string.media_open_with))
    }

    /** Shares a single [item] through the system share sheet via `ACTION_SEND`. */
    fun share(
        context: Context,
        item: FileItem,
    ): Boolean = shareAll(context, listOf(item))

    /**
     * Shares one or more [items] through the system share sheet. Directories and
     * non-content locators are dropped (they cannot be streamed to another app);
     * an empty result is a no-op returning `false`.
     */
    fun shareAll(
        context: Context,
        items: List<FileItem>,
    ): Boolean {
        val shareable = shareableItems(items)
        if (shareable.isEmpty()) return false

        val uris = shareable.map { Uri.parse(it.path) }
        val plan = ShareIntentPlanner.plan(shareable)
        val intent =
            if (plan.isMultiple) {
                Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    type = plan.mimeType
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                }
            } else {
                Intent(Intent.ACTION_SEND).apply {
                    type = plan.mimeType
                    putExtra(Intent.EXTRA_STREAM, uris.first())
                }
            }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        // A ClipData spanning every URI so the read grant propagates to all of them.
        intent.clipData =
            ClipData.newRawUri(shareable.first().name, uris.first()).apply {
                uris.drop(1).forEach { addItem(ClipData.Item(it)) }
            }
        return dispatch(context, intent, context.getString(R.string.media_share_with))
    }

    /**
     * A file is openable/shareable when it is a non-directory addressed by a content
     * URI — directories have no viewer and raw filesystem paths cannot be granted to
     * another app without FileProvider staging (which media items never need).
     */
    private fun isOpenable(item: FileItem): Boolean = !item.isDirectory && item.path.startsWith(CONTENT_SCHEME)

    /**
     * The subset of [items] that can be streamed to another app. Pure so the filter
     * rule is unit-tested without the Android intent layer.
     */
    internal fun shareableItems(items: List<FileItem>): List<FileItem> = items.filter(::isOpenable)

    private fun dispatch(
        context: Context,
        intent: Intent,
        title: String,
    ): Boolean {
        val chooser =
            Intent.createChooser(intent, title).apply {
                // The launching context may be the application context, not an Activity.
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        return try {
            context.startActivity(chooser)
            true
        } catch (e: ActivityNotFoundException) {
            // No handler installed — surface gracefully instead of crashing.
            false
        }
    }
}
