package com.appblish.filora.core.ui.a11y

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * Minimum interactive target edge — WCAG 2.5.5 (Target Size) and the Material
 * touch-target guidance (NFR-5). Custom clickable surfaces (grid tiles, list
 * rows) do not get this for free the way [androidx.compose.material3.IconButton]
 * does, so they must opt in via [clickableTile].
 */
val MinTouchTargetSize = 48.dp

/**
 * Makes the receiver a single TalkBack-focusable button:
 *
 * - [clickable] collapses the child text/icon into one semantics node, so the
 *   reader announces the whole tile ("Images, 240 items") instead of each
 *   fragment separately.
 * - [onClickLabel] becomes the spoken action ("double tap to open Images")
 *   rather than the generic "double tap to activate". Pass a localized
 *   `stringResource(...)` so the announcement honours NFR-7.
 * - [role] exposes the node as a [Role.Button] so the control type is announced.
 * - [heightIn] guarantees the 48dp minimum touch target even when the visual
 *   content is shorter (e.g. a one-line storage category row).
 *
 * Prefer this over a bare `Modifier.clickable {}` for any custom tappable
 * surface in a core flow.
 */
fun Modifier.clickableTile(
    onClickLabel: String,
    role: Role = Role.Button,
    onClick: () -> Unit,
): Modifier =
    this
        .heightIn(min = MinTouchTargetSize)
        .clickable(onClickLabel = onClickLabel, role = role, onClick = onClick)
