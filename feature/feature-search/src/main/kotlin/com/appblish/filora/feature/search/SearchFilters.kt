package com.appblish.filora.feature.search

import androidx.annotation.StringRes
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
    @StringRes val labelRes: Int,
    val minBytes: Long?,
    val maxBytes: Long?,
) {
    Small(R.string.search_size_small, null, MB),
    Medium(R.string.search_size_medium, MB, 100 * MB),
    Large(R.string.search_size_large, 100 * MB, null),
}

/**
 * User-facing "modified within" presets (FR-5.2). Each is a window counted back from
 * a supplied `now`, so the bound stays testable without reading the wall clock here.
 */
enum class DateBucket(
    @StringRes val labelRes: Int,
    val windowMillis: Long,
) {
    Today(R.string.search_date_today, DAY_MILLIS),
    Week(R.string.search_date_week, 7 * DAY_MILLIS),
    Month(R.string.search_date_month, 30 * DAY_MILLIS),
    Year(R.string.search_date_year, 365 * DAY_MILLIS),
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
    @get:StringRes
    val labelRes: Int

    data class Type(
        val type: FileTypeFilter
    ) : ActiveFilterChip {
        override val labelRes: Int get() = type.chipLabelRes
    }

    data class Size(
        val bucket: SizeBucket
    ) : ActiveFilterChip {
        override val labelRes: Int get() = bucket.labelRes
    }

    data class Date(
        val bucket: DateBucket
    ) : ActiveFilterChip {
        override val labelRes: Int get() = bucket.labelRes
    }
}

/** Short, title-case label for a type chip. */
@get:StringRes
val FileTypeFilter.chipLabelRes: Int
    get() =
        when (this) {
            FileTypeFilter.Image -> R.string.search_type_images
            FileTypeFilter.Video -> R.string.search_type_video
            FileTypeFilter.Audio -> R.string.search_type_audio
            FileTypeFilter.Document -> R.string.search_type_docs
            FileTypeFilter.Archive -> R.string.search_type_archives
            FileTypeFilter.Apk -> R.string.search_type_apks
        }
