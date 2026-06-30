package com.appblish.filora.core.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Filora spacing tokens — a 4dp base scale (hi-fi spec §1.3, A-blend).
 *
 * Reference these instead of hard-coding `.dp` in feature screens; ad-hoc gaps are the
 * #1 source of visual inconsistency. Usage contract (spec §1.3):
 * - Screen edge padding: [lg] (16dp) phone, [xl] (24dp) at ≥600dp.
 * - List row vertical padding: [md] (12dp) → row height ≥ 56dp (comfortable density).
 * - Between cards / section gap: [lg]–[xl].
 * - Inside a card: [lg] padding, [sm]–[md] between elements.
 * - Grid gutter (media): [sm] (8dp).
 */
object FiloraSpacing {
    val none = 0.dp
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 48.dp
}
