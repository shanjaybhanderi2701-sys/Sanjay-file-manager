package com.appblish.filora.core.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Filora tonal-elevation tokens (hi-fi spec §1.4, M3 Expressive).
 *
 * M3 Expressive favors **tonal** elevation (a surface-color shift driven by dynamic
 * color) over drop shadows. Pass these to `Surface(tonalElevation = …)` — never as a
 * shadow — so Material You tints elevation correctly.
 *
 * | Token   | Use                                                            |
 * |---------|----------------------------------------------------------------|
 * | [level0]| Screen background, scrolled-under list                         |
 * | [level1]| Resting cards, list (Home category/storage cards)              |
 * | [level2]| App bar on scroll, search bar, batch-action bar                |
 * | [level3]| FAB, menus, the **selected** pane in dual-pane                 |
 * | [level4]| Dialogs, bottom sheets                                         |
 * | [level5]| The conflict sheet                                             |
 *
 * Active selection state = [level3] tonal + `secondaryContainer` tint (spec §2).
 */
object FiloraElevation {
    val level0 = 0.dp
    val level1 = 1.dp
    val level2 = 3.dp
    val level3 = 6.dp
    val level4 = 8.dp
    val level5 = 12.dp
}
