package com.appblish.filora.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Filora typography — the Material 3 type scale, rendered with the platform default
 * family (Roboto / device sans) so the app looks native and ships no bundled font in
 * v1. Sizes/line-heights/weights mirror the authoritative design system
 * (`docs/design/01-design-system.md` §4). A brand font can be swapped in centrally
 * here later without touching call sites.
 *
 * Only the roles Filora actually uses are overridden; any role left unset falls back
 * to the Material 3 default for that role.
 */
private val Default = FontFamily.Default

val FiloraTypography =
    Typography(
        // Display — storage hero numbers.
        displaySmall =
            TextStyle(
                fontFamily = Default,
                fontWeight = FontWeight.Normal,
                fontSize = 36.sp,
                lineHeight = 44.sp,
            ),
        // Headline — empty-state titles, large dialog titles.
        headlineSmall =
            TextStyle(
                fontFamily = Default,
                fontWeight = FontWeight.Normal,
                fontSize = 24.sp,
                lineHeight = 32.sp,
            ),
        // Title — app-bar titles, section headers, grid-tile labels.
        titleLarge =
            TextStyle(
                fontFamily = Default,
                fontWeight = FontWeight.Normal,
                fontSize = 22.sp,
                lineHeight = 28.sp,
            ),
        titleMedium =
            TextStyle(
                fontFamily = Default,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                lineHeight = 24.sp,
            ),
        titleSmall =
            TextStyle(
                fontFamily = Default,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            ),
        // Body — file/folder names (bodyLarge), descriptions (bodyMedium),
        // file metadata and captions (bodySmall).
        bodyLarge =
            TextStyle(
                fontFamily = Default,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
            ),
        bodyMedium =
            TextStyle(
                fontFamily = Default,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            ),
        bodySmall =
            TextStyle(
                fontFamily = Default,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            ),
        // Label — buttons/chips (labelLarge), crumbs/chip labels (labelMedium),
        // badge counts and overlines (labelSmall).
        labelLarge =
            TextStyle(
                fontFamily = Default,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            ),
        labelMedium =
            TextStyle(
                fontFamily = Default,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            ),
        labelSmall =
            TextStyle(
                fontFamily = Default,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                lineHeight = 16.sp,
            ),
    )
