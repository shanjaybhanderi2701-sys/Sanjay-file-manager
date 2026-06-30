package com.appblish.filora.core.common.util

import java.util.Locale

/**
 * Pure, offline MIME-type reasoning for open/share intents (FR-10). No Android
 * dependency, so it stays unit-testable on the JVM; the framework `MimeTypeMap`
 * is intentionally avoided to keep this in `core-common`.
 *
 * The table is deliberately small — the common file kinds a file manager opens or
 * shares. The data layer usually supplies a precise `mimeType` from MediaStore;
 * this fills the gap for plain filesystem entries and computes the single type a
 * multi-file share sheet should advertise.
 */
object MimeTypes {
    private const val STAR = "*"

    // Used for ACTION_VIEW/ACTION_SEND when no narrower type is known, so the
    // chooser still appears for unknown types (FR-10.1). Built from [STAR] to keep
    // the source free of slash-star sequences.
    val WILDCARD = "$STAR/$STAR"

    // Lowercased, dot-less extension -> concrete MIME type.
    private val BY_EXTENSION =
        mapOf(
            // images
            "jpg" to "image/jpeg",
            "jpeg" to "image/jpeg",
            "png" to "image/png",
            "gif" to "image/gif",
            "webp" to "image/webp",
            "bmp" to "image/bmp",
            "heic" to "image/heic",
            "heif" to "image/heif",
            "svg" to "image/svg+xml",
            // video
            "mp4" to "video/mp4",
            "m4v" to "video/mp4",
            "mkv" to "video/x-matroska",
            "webm" to "video/webm",
            "avi" to "video/x-msvideo",
            "mov" to "video/quicktime",
            "3gp" to "video/3gpp",
            "flv" to "video/x-flv",
            // audio
            "mp3" to "audio/mpeg",
            "wav" to "audio/x-wav",
            "flac" to "audio/flac",
            "aac" to "audio/aac",
            "ogg" to "audio/ogg",
            "opus" to "audio/ogg",
            "m4a" to "audio/mp4",
            "wma" to "audio/x-ms-wma",
            // documents
            "pdf" to "application/pdf",
            "doc" to "application/msword",
            "docx" to "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "xls" to "application/vnd.ms-excel",
            "xlsx" to "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "ppt" to "application/vnd.ms-powerpoint",
            "pptx" to "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "txt" to "text/plain",
            "md" to "text/markdown",
            "rtf" to "application/rtf",
            "odt" to "application/vnd.oasis.opendocument.text",
            "csv" to "text/csv",
            // archives
            "zip" to "application/zip",
            "rar" to "application/vnd.rar",
            "7z" to "application/x-7z-compressed",
            "tar" to "application/x-tar",
            "gz" to "application/gzip",
            "bz2" to "application/x-bzip2",
            "xz" to "application/x-xz",
        )

    /** Concrete MIME for [extension] (dot-less, any case), or null if unknown. */
    fun fromExtension(extension: String): String? = BY_EXTENSION[extension.lowercase(Locale.ROOT)]

    /**
     * Best MIME type for a single entry: the data layer's [declaredMime] when
     * present and concrete, otherwise inferred from [extension], otherwise
     * [WILDCARD]. A blank or already-wildcard [declaredMime] is treated as
     * "unknown" so the extension still gets a chance.
     */
    fun resolve(
        declaredMime: String?,
        extension: String,
    ): String {
        val declared = declaredMime?.trim().orEmpty()
        if (declared.isNotEmpty() && !declared.endsWith(STAR)) {
            return declared
        }
        return fromExtension(extension) ?: declared.ifEmpty { WILDCARD }
    }

    /**
     * The single type a share sheet should advertise for a set of files: the exact
     * type when every file matches it, else the shared top-level type as a wildcard
     * such as `image` followed by a star, else [WILDCARD] for a mixed set. An empty
     * input yields [WILDCARD].
     */
    fun commonType(mimeTypes: List<String>): String {
        if (mimeTypes.isEmpty()) return WILDCARD
        val distinct = mimeTypes.toSet()
        if (distinct.size == 1) return distinct.first()
        val topLevels = distinct.map { it.substringBefore('/') }.toSet()
        return if (topLevels.size == 1 && topLevels.first().isNotEmpty()) {
            "${topLevels.first()}/$STAR"
        } else {
            WILDCARD
        }
    }
}
