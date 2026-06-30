package com.appblish.filora.feature.browser.share

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.appblish.filora.core.domain.model.FileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Turns a [FileItem] into a `content://` [Uri] that other apps may read (FR-10,
 * NFR-3.3). Two cases:
 *
 *  * The item is already addressed by a content URI (MediaStore item or SAF
 *    document) — pass it straight through; the receiving app reads it under the
 *    grant we attach to the intent.
 *  * The item is a plain filesystem path — it is **staged** into the app cache's
 *    `shared/` directory (the only root `file_paths.xml` exposes) and served via
 *    [FileProvider]. We never expose arbitrary external paths through the provider,
 *    so a copy is the deliberate, scoped way to hand a raw file to another app.
 *
 * The provider authority mirrors the manifest declaration `${applicationId}.fileprovider`.
 */
object ShareUriResolver {
    private const val STAGE_DIR = "shared"

    private fun authorityFor(context: Context): String = "${context.packageName}.fileprovider"

    /** True when [path] is a content URI we can forward without copying. */
    private fun isContentUri(path: String): Boolean = path.startsWith("content://")

    /**
     * Resolves a single [item] to a shareable [Uri], staging a filesystem entry into
     * the cache when needed. Runs file I/O off the main thread.
     */
    suspend fun resolve(
        context: Context,
        item: FileItem,
    ): Uri =
        if (isContentUri(item.path)) {
            Uri.parse(item.path)
        } else {
            withContext(Dispatchers.IO) { stage(context, item) }
        }

    /** Resolves [items] in order, reusing a single staging directory. */
    suspend fun resolveAll(
        context: Context,
        items: List<FileItem>,
    ): List<Uri> = items.map { resolve(context, it) }

    private fun stage(
        context: Context,
        item: FileItem,
    ): Uri {
        val source = File(item.path)
        val stageDir = File(context.cacheDir, STAGE_DIR).apply { mkdirs() }
        val staged = File(stageDir, item.name)
        // Re-copy only when the cached copy is stale, so repeated shares are cheap.
        val stale =
            !staged.exists() ||
                staged.length() != source.length() ||
                staged.lastModified() < source.lastModified()
        if (stale) {
            source.copyTo(staged, overwrite = true)
        }
        return FileProvider.getUriForFile(context, authorityFor(context), staged)
    }
}
