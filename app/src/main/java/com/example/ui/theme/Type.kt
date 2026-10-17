package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Nothing OS Dot-Matrix / NDot Spaced Typography Style
val DotMatrixFont = FontFamily.Monospace

val DotMatrixHeadingLarge = TextStyle(
    fontFamily = DotMatrixFont,
    fontWeight = FontWeight.Bold,
    fontSize = 24.sp,
    letterSpacing = 2.5.sp
)

val DotMatrixHeadingMedium = TextStyle(
    fontFamily = DotMatrixFont,
    fontWeight = FontWeight.Bold,
    fontSize = 18.sp,
    letterSpacing = 2.0.sp
)

val DotMatrixSectionTitle = TextStyle(
    fontFamily = DotMatrixFont,
    fontWeight = FontWeight.SemiBold,
    fontSize = 13.sp,
    letterSpacing = 2.0.sp
)

val DotMatrixBadge = TextStyle(
    fontFamily = DotMatrixFont,
    fontWeight = FontWeight.Medium,
    fontSize = 11.sp,
    letterSpacing = 1.2.sp
)

val Typography = Typography(
    headlineLarge = DotMatrixHeadingLarge,
    headlineMedium = DotMatrixHeadingMedium,
    titleMedium = DotMatrixSectionTitle,
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.3.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.2.sp
    ),
    labelSmall = DotMatrixBadge
)
