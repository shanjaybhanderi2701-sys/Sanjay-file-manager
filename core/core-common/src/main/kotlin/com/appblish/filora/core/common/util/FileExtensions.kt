package com.appblish.filora.core.common.util

import java.util.Locale

/** Pure helpers for reasoning about file names/extensions. No I/O, no Android. */
object FileExtensions {
    private val IMAGE = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif", "svg")
    private val VIDEO = setOf("mp4", "mkv", "webm", "avi", "mov", "3gp", "flv", "m4v")
    private val AUDIO = setOf("mp3", "wav", "flac", "aac", "ogg", "m4a", "opus", "wma")
    private val DOCUMENT =
        setOf(
            "pdf",
            "doc",
            "docx",
            "xls",
            "xlsx",
            "ppt",
            "pptx",
            "txt",
            "md",
            "rtf",
            "odt",
            "csv",
        )
    private val ARCHIVE = setOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz")
    private const val APK = "apk"

    /** Lowercased extension without the dot, or empty string if none. */
    fun extensionOf(name: String): String {
        val dot = name.lastIndexOf('.')
        if (dot <= 0 || dot == name.lastIndex) return ""
        return name.substring(dot + 1).lowercase(Locale.ROOT)
    }

    /** File name without its trailing extension. */
    fun baseName(name: String): String {
        val dot = name.lastIndexOf('.')
        return if (dot <= 0) name else name.substring(0, dot)
    }

    fun isImage(name: String): Boolean = extensionOf(name) in IMAGE

    fun isVideo(name: String): Boolean = extensionOf(name) in VIDEO

    fun isAudio(name: String): Boolean = extensionOf(name) in AUDIO

    fun isDocument(name: String): Boolean = extensionOf(name) in DOCUMENT

    fun isArchive(name: String): Boolean = extensionOf(name) in ARCHIVE

    /**
     * Coarse [FileCategory] for [name], classified by extension only (cheap, always
     * present). The search/filter layer (FR-5.2) uses this to bucket a result by type
     * without consulting MIME or touching I/O; an unrecognized extension is
     * [FileCategory.Other].
     */
    fun categoryOf(name: String): FileCategory =
        when {
            isImage(name) -> FileCategory.Image
            isVideo(name) -> FileCategory.Video
            isAudio(name) -> FileCategory.Audio
            isDocument(name) -> FileCategory.Document
            isArchive(name) -> FileCategory.Archive
            extensionOf(name) == APK -> FileCategory.Apk
            else -> FileCategory.Other
        }
}

/**
 * Extension-derived file class, mirrored from the search type chips (FR-5.2). Kept in
 * `core-common` (Android-free) so both the data walk and the domain filter can bucket
 * an entry without depending on a richer model.
 */
enum class FileCategory {
    Image,
    Video,
    Audio,
    Document,
    Archive,
    Apk,
    Other,
}
