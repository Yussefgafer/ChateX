package com.kai.ghostmesh.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Using System Sans Serif as base, assuming system handles Poppins/Inter via dynamic assets if present.
// Otherwise, we use standard Sans Serif with correct weights.
val TitleFontFamily = FontFamily.SansSerif
val BodyFontFamily = FontFamily.SansSerif

fun createTypography(scale: Float): Typography {
    val s = scale.coerceIn(0.8f, 1.5f)
    return Typography(
        bodyLarge = TextStyle(
            fontFamily = BodyFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (16 * s).sp,
            lineHeight = (24 * s).sp,
            letterSpacing = 0.5.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = BodyFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (14 * s).sp,
            lineHeight = (20 * s).sp,
            letterSpacing = 0.25.sp
        ),
        bodySmall = TextStyle(
            fontFamily = BodyFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (12 * s).sp,
            lineHeight = (16 * s).sp,
            letterSpacing = 0.4.sp
        ),
        titleLarge = TextStyle(
            fontFamily = TitleFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (22 * s).sp,
            lineHeight = (28 * s).sp,
            letterSpacing = 0.sp
        ),
        titleMedium = TextStyle(
            fontFamily = TitleFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (16 * s).sp,
            lineHeight = (24 * s).sp,
            letterSpacing = 0.15.sp
        ),
        titleSmall = TextStyle(
            fontFamily = TitleFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (14 * s).sp,
            lineHeight = (20 * s).sp,
            letterSpacing = 0.1.sp
        ),
        labelLarge = TextStyle(
            fontFamily = BodyFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (14 * s).sp,
            lineHeight = (20 * s).sp,
            letterSpacing = 0.1.sp
        ),
        labelMedium = TextStyle(
            fontFamily = BodyFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (12 * s).sp,
            lineHeight = (16 * s).sp,
            letterSpacing = 0.5.sp
        ),
        labelSmall = TextStyle(
            fontFamily = BodyFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (11 * s).sp,
            lineHeight = (16 * s).sp,
            letterSpacing = 0.5.sp
        )
    )
}

val Typography = createTypography(1.0f)
