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
}
