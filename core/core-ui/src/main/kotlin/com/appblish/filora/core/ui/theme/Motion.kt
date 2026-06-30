package com.appblish.filora.core.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing

/**
 * Filora motion tokens (hi-fi spec §1.6, M3 Expressive but restrained).
 *
 * A-blend motion is **functional, not decorative** — principle #3 caps Direction C's
 * generous motion. Durations are in milliseconds; easings are the M3 Expressive set.
 *
 * | Pattern              | Duration            | Easing                 |
 * |----------------------|---------------------|------------------------|
 * | Open file/folder     | [containerTransform]| [Emphasized]           |
 * | Screen change        | [screenChange]      | [Emphasized]           |
 * | Dialog / sheet in    | [dialogEnter]       | [EmphasizedDecelerate] |
 * | Selection enter      | [selectionEnter]    | [Standard]             |
 * | Skeleton → content   | [stateCrossfade]    | [Standard]             |
 * | Storage-story reveal | [storyReveal] (cap) | [EmphasizedDecelerate] |
 *
 * **Hard rule (principle #3):** no continuous/parallax motion, nothing that runs while
 * scrolling, thumbnails never animate in on scroll. The storage-story is one-shot on
 * first composition only and must honor the system animation scale / reduce-motion.
 */
object FiloraMotion {
    const val containerTransform = 350
    const val screenChange = 300
    const val dialogEnter = 200
    const val selectionEnter = 150
    const val stateCrossfade = 150
    const val storyReveal = 600

    /** M3 Expressive emphasized easing — standard in-and-out for high-emphasis motion. */
    val Emphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** Emphasized decelerate — for elements entering the screen (dialogs, story reveal). */
    val EmphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    /** Emphasized accelerate — for elements leaving the screen. */
    val EmphasizedAccelerate: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    /** Standard easing — small functional transitions (selection, cross-fades). */
    val Standard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
}
