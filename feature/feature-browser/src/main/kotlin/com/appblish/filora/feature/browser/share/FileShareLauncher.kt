package com.appblish.filora.feature.browser.share

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.appblish.filora.core.domain.model.FileItem
import com.appblish.filora.core.domain.usecase.ShareIntentPlanner

/**
 * Builds and dispatches open/share intents for files (FR-10.1, FR-10.2).
 *
 * Every outgoing intent carries [Intent.FLAG_GRANT_READ_URI_PERMISSION] and a
 * [ClipData] so the read grant reaches the handler — and only the handler — for the
 * single content URI or the whole batch (NFR-3.3: scoped, time-bounded grants). The
 * actual `content://` URIs come from [ShareUriResolver], which staging-copies plain
 * filesystem entries into the cache so nothing outside `file_paths.xml` is ever exposed.
 *
 * Dispatch is always wrapped in a chooser and guarded against
 * [ActivityNotFoundException], so an unknown type still offers the chooser and a
 * device with no handler degrades to a `false` return instead of a crash (FR-10.1 AC).
 */
object FileShareLauncher {
    /**
     * Opens a single [item] in another app via `ACTION_VIEW`. Returns false when no
     * activity can handle it (the caller can then surface a "no app found" message).
     */
    suspend fun openFile(
        context: Context,
        item: FileItem,
    ): Boolean {
        val uri = ShareUriResolver.resolve(context, item)
        val intent =
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, ShareIntentPlanner.openType(item))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                clipData = ClipData.newRawUri(item.name, uri)
            }
        return dispatch(context, intent, title = "Open with")
    }

    /**
     * Shares one or more files through the system share sheet. Directories are
     * dropped (they cannot be sent via `ACTION_SEND`); an empty result is a no-op
     * returning false.
     */
    suspend fun shareFiles(
        context: Context,
        items: List<FileItem>,
    ): Boolean {
        val files = items.filterNot { it.isDirectory }
        if (files.isEmpty()) return false

        val uris = ShareUriResolver.resolveAll(context, files)
        val plan = ShareIntentPlanner.plan(files)
        val intent = if (plan.isMultiple) multiSend(uris, plan.mimeType) else singleSend(uris.first(), plan.mimeType)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.clipData = clipDataFor(files, uris)
        return dispatch(context, intent, title = "Share")
    }

    private fun singleSend(
        uri: Uri,
        mimeType: String,
    ): Intent =
        Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
        }

    private fun multiSend(
        uris: List<Uri>,
        mimeType: String,
    ): Intent =
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = mimeType
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        }

    /** A [ClipData] spanning every shared URI so the read grant propagates to all. */
    private fun clipDataFor(
        files: List<FileItem>,
        uris: List<Uri>,
    ): ClipData {
        val clip = ClipData.newRawUri(files.first().name, uris.first())
        uris.drop(1).forEach { clip.addItem(ClipData.Item(it)) }
        return clip
    }

    private fun dispatch(
        context: Context,
        intent: Intent,
        title: String,
    ): Boolean {
        val chooser =
            Intent.createChooser(intent, title).apply {
                // The launching context may not be an Activity (e.g. application context).
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        return try {
            context.startActivity(chooser)
            true
        } catch (e: ActivityNotFoundException) {
            // No handler installed — surface gracefully instead of crashing (FR-10.1 AC).
            false
        }
    }
}
