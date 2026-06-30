package com.appblish.filora.feature.search

import com.appblish.filora.core.domain.model.FileTypeFilter

/** A megabyte, the unit the size buckets are expressed in. */
private const val MB = 1_048_576L

/** A day in milliseconds, the unit the date buckets count back from "now". */
private const val DAY_MILLIS = 24L * 60 * 60 * 1000

/**
 * User-facing size presets (FR-5.2). Each maps to a primitive byte range on
 * [com.appblish.filora.core.domain.model.SearchFilter]; selection is single-choice
 * because the three buckets partition the size axis.
 */
enum class SizeBucket(
    val label: String,
    val minBytes: Long?,
    val maxBytes: Long?,
) {
    Small("Under 1 MB", null, MB),
    Medium("1–100 MB", MB, 100 * MB),
    Large("Over 100 MB", 100 * MB, null),
}

/**
 * User-facing "modified within" presets (FR-5.2). Each is a window counted back from
 * a supplied `now`, so the bound stays testable without reading the wall clock here.
 */
enum class DateBucket(
    val label: String,
    val windowMillis: Long,
) {
    Today("Today", DAY_MILLIS),
    Week("Last 7 days", 7 * DAY_MILLIS),
    Month("Last 30 days", 30 * DAY_MILLIS),
    Year("Last year", 365 * DAY_MILLIS),
    ;

    /** Earliest modification time that still falls inside this window relative to [now]. */
    fun afterBound(now: Long): Long = now - windowMillis
}

/**
 * One active filter rendered as a removable chip (FR-5.2: "active filters shown as
 * removable chips"). Tapping a chip's close affordance clears exactly its dimension
 * via [SearchViewModel.removeChip].
 */
sealed interface ActiveFilterChip {
    val label: String

    data class Type(
        val type: FileTypeFilter
    ) : ActiveFilterChip {
        override val label: String get() = type.chipLabel
    }

    data class Size(
        val bucket: SizeBucket
    ) : ActiveFilterChip {
        override val label: String get() = bucket.label
    }

    data class Date(
        val bucket: DateBucket
    ) : ActiveFilterChip {
        override val label: String get() = bucket.label
    }
}

/** Short, title-case label for a type chip. */
val FileTypeFilter.chipLabel: String
    get() =
        when (this) {
            FileTypeFilter.Image -> "Images"
            FileTypeFilter.Video -> "Video"
            FileTypeFilter.Audio -> "Audio"
            FileTypeFilter.Document -> "Docs"
            FileTypeFilter.Archive -> "Archives"
            FileTypeFilter.Apk -> "APKs"
        }
