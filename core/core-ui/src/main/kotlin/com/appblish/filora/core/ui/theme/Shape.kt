package com.appblish.filora.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Filora shape tokens — rounded, friendly corners per the design system. */
val FiloraShapes =
    Shapes(
        extraSmall = RoundedCornerShape(4.dp),
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(12.dp),
        large = RoundedCornerShape(16.dp),
        extraLarge = RoundedCornerShape(28.dp),
    )
