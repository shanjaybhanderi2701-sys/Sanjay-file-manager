package com.appblish.filora.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Filora typography. Uses the platform default family for M1; a brand font can be
 * swapped in centrally here later without touching call sites.
 */
private val Default = FontFamily.Default

val FiloraTypography =
    Typography(
        titleLarge =
            TextStyle(
                fontFamily = Default,
                fontWeight = FontWeight.SemiBold,
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
        labelMedium =
            TextStyle(
                fontFamily = Default,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            ),
    )
