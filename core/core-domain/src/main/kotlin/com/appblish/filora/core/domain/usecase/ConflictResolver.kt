package com.appblish.filora.core.domain.usecase

import com.appblish.filora.core.domain.model.ConflictStrategy

/**
 * Decision for a single source about to be written into a destination directory.
 */
sealed interface ConflictResolution {
    /** Do not write this source ([ConflictStrategy.Skip] hit a collision). */
    data object Skip : ConflictResolution

    /**
     * Write the source as [name], replacing any existing entry only when
     * [overwrite] is true. A [name] free of collisions (or a keep-both name) is
     * written with [overwrite] = false; a [ConflictStrategy.Replace] collision is
     * written with [overwrite] = true.
     */
    data class Proceed(
        val name: String,
        val overwrite: Boolean,
    ) : ConflictResolution
}

/**
 * Pure resolution of name collisions for copy/move (FR-3.3). Matching is
 * case-insensitive to stay consistent with [FileNameValidator] and FAT/exFAT
 * external volumes, so "Report.pdf" collides with "report.pdf".
 *
 * When [strategy] is [ConflictStrategy.KeepBoth] the resolver appends an
 * incrementing " (n)" suffix before the extension — "report.pdf" becomes
 * "report (1).pdf" — until a free name is found, reserving against names already
 * claimed earlier in the same batch.
 */
object ConflictResolver {
    fun resolve(
        sourceName: String,
        isDirectory: Boolean,
        existingNames: Set<String>,
        strategy: ConflictStrategy,
    ): ConflictResolution {
        if (!collides(sourceName, existingNames)) {
            return ConflictResolution.Proceed(sourceName, overwrite = false)
        }
        return when (strategy) {
            ConflictStrategy.Skip -> ConflictResolution.Skip
            ConflictStrategy.Replace -> ConflictResolution.Proceed(sourceName, overwrite = true)
            ConflictStrategy.KeepBoth ->
                ConflictResolution.Proceed(
                    name = uniqueName(sourceName, isDirectory, existingNames),
                    overwrite = false,
                )
        }
    }

    private fun collides(
        name: String,
        existingNames: Set<String>,
    ): Boolean = existingNames.any { it.equals(name, ignoreCase = true) }

    private fun uniqueName(
        name: String,
        isDirectory: Boolean,
        existingNames: Set<String>,
    ): String {
        val (base, extension) = splitName(name, isDirectory)
        var counter = 1
        while (true) {
            val candidate = "$base ($counter)$extension"
            if (!collides(candidate, existingNames)) return candidate
            counter++
        }
    }

    /**
     * Splits [name] into a base and an extension (including the leading dot).
     * Directories are never split. A leading dot ("`.gitignore`") is treated as a
     * hidden name with no extension, so the suffix lands at the end.
     */
    private fun splitName(
        name: String,
        isDirectory: Boolean,
    ): Pair<String, String> {
        if (isDirectory) return name to ""
        val dot = name.lastIndexOf('.')
        return if (dot > 0) {
            name.substring(0, dot) to name.substring(dot)
        } else {
            name to ""
        }
    }
}
