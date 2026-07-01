package com.appblish.filora.core.domain.model

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

/**
 * Recycle-bin retention policy (FR-3.4, T128). Items whose delete timestamp is older
 * than [maxAge] are eligible for auto-purge. The default is 30 days; the value is a
 * single knob so a future settings screen can make it user-configurable without
 * touching the purge logic.
 */
data class TrashRetention(
    val maxAge: Duration = DEFAULT_MAX_AGE,
) {
    companion object {
        /** Android/desktop file managers converge on ~30 days; we match that. */
        val DEFAULT_MAX_AGE: Duration = 30.days

        val Default = TrashRetention()
    }
}
