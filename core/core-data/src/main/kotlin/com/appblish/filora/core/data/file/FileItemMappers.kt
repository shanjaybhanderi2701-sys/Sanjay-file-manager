package com.appblish.filora.core.data.file

import androidx.documentfile.provider.DocumentFile
import com.appblish.filora.core.common.util.FileExtensions
import com.appblish.filora.core.domain.model.FileItem
import java.io.File

/**
 * Source → [FileItem] mappers (T033). Both the java.io ([FileSystemDataSource]) and SAF
 * ([SafDataSource]) listings funnel through these so the rest of the app sees one
 * platform-neutral model. Directory size is reported as 0 (computing a recursive size
 * during a listing would defeat the 10k-entry browse budget, NFR-1); child counts are
 * left null for the same reason.
 */
internal fun File.toFileItem(): FileItem =
    FileItem(
        name = name,
        path = absolutePath,
        isDirectory = isDirectory,
        sizeBytes = if (isDirectory) 0L else length(),
        lastModifiedEpochMillis = lastModified(),
        extension = FileExtensions.extensionOf(name),
        isHidden = name.startsWith(HIDDEN_PREFIX),
    )

/**
 * Maps a SAF entry, addressed by its document URI. Returns null when the entry has no
 * resolvable name (a transient SAF state) so the caller can skip it rather than surface a
 * blank row.
 */
internal fun DocumentFile.toFileItem(): FileItem? {
    val entryName = name ?: return null
    return FileItem(
        name = entryName,
        path = uri.toString(),
        isDirectory = isDirectory,
        sizeBytes = if (isDirectory) 0L else length(),
        lastModifiedEpochMillis = lastModified(),
        extension = FileExtensions.extensionOf(entryName),
        isHidden = entryName.startsWith(HIDDEN_PREFIX),
    )
}

/** Unix dotfile convention; the show-hidden toggle (FR-2.4) keys off this. */
private const val HIDDEN_PREFIX = "."
