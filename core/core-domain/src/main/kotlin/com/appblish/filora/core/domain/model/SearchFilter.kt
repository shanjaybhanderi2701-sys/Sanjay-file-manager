package com.appblish.filora.core.domain.model

import com.appblish.filora.core.common.util.FileExtensions

/**
 * Post-match filtering applied on top of the name-substring search stream
 * ([com.appblish.filora.core.domain.usecase.SearchFilesUseCase], T5.1) to satisfy
 * FR-5.2: filter results by file type, size range, and modification-date range.
 *
 * The three dimensions **combine with AND** — an item is kept only when it passes
 * every active dimension. Within the type dimension multiple selections are OR'd
 * (a file matches if it is *any* of the chosen types), which is what a user expects
 * from a set of type chips. An unset dimension (no types, null bound) is a no-op and
 * lets everything through, so the empty filter ([isEmpty]) keeps the whole stream.
 *
 * Pure and Android-free: the screen maps chip presets onto these primitive bounds,
 * and the matcher is unit-tested independently of any UI or data source.
 */
data class SearchFilter(
    val types: Set<FileTypeFilter> = emptySet(),
    val minSizeBytes: Long? = null,
    val maxSizeBytes: Long? = null,
    val modifiedAfterEpochMillis: Long? = null,
    val modifiedBeforeEpochMillis: Long? = null,
) {
    /** True when no dimension is constrained, so [matches] accepts every item. */
    val isEmpty: Boolean
        get() =
            types.isEmpty() &&
                minSizeBytes == null &&
                maxSizeBytes == null &&
                modifiedAfterEpochMillis == null &&
                modifiedBeforeEpochMillis == null

    /**
     * Keeps [item] only when it passes every active dimension (AND). Directories are
     * excluded as soon as any dimension is active: a folder has no size or meaningful
     * type, so a typed/size/date filter is implicitly "files only".
     */
    fun matches(item: FileItem): Boolean {
        if (isEmpty) return true
        if (item.isDirectory) return false
        return matchesType(item) && matchesSize(item) && matchesDate(item)
    }

    private fun matchesType(item: FileItem): Boolean = types.isEmpty() || types.any { it.matches(item) }

    private fun matchesSize(item: FileItem): Boolean {
        if (minSizeBytes != null && item.sizeBytes < minSizeBytes) return false
        if (maxSizeBytes != null && item.sizeBytes > maxSizeBytes) return false
        return true
    }

    private fun matchesDate(item: FileItem): Boolean {
        val modified = item.lastModifiedEpochMillis
        if (modifiedAfterEpochMillis != null && modified < modifiedAfterEpochMillis) return false
        if (modifiedBeforeEpochMillis != null && modified > modifiedBeforeEpochMillis) return false
        return true
    }
}

/**
 * The six file types a user can filter on (FR-5.2). Classification is by extension
 * first (cheap, always present) and MIME prefix as a fallback for extension-less
 * names, mirroring [com.appblish.filora.core.data.media.MediaClassifier] but pure
 * over a [FileItem].
 */
enum class FileTypeFilter {
    Image,
    Video,
    Audio,
    Document,
    Archive,
    Apk,
    ;

    fun matches(item: FileItem): Boolean {
        val mime = item.mimeType?.lowercase().orEmpty()
        return when (this) {
            Image -> FileExtensions.isImage(item.name) || mime.startsWith("image/")
            Video -> FileExtensions.isVideo(item.name) || mime.startsWith("video/")
            Audio -> FileExtensions.isAudio(item.name) || mime.startsWith("audio/")
            Document -> FileExtensions.isDocument(item.name) || mime.startsWith("text/")
            Archive -> FileExtensions.isArchive(item.name)
            Apk -> FileExtensions.extensionOf(item.name) == APK_EXTENSION || mime == APK_MIME
        }
    }

    private companion object {
        const val APK_EXTENSION = "apk"
        const val APK_MIME = "application/vnd.android.package-archive"
    }
}
