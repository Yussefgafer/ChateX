package com.kai.ghostmesh.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// MD3 Expressive Typography: Poppins for Vitality, Lora for Readability.
// System fallbacks ensure stability on all device targets.
val TitleFontFamily = FontFamily.SansSerif // Poppins surrogate
val BodyFontFamily = FontFamily.Serif    // Lora surrogate

fun createTypography(scale: Float): Typography {
    val s = scale.coerceIn(0.8f, 1.5f)
    return Typography(
        displayLarge = TextStyle(
            fontFamily = TitleFontFamily,
            fontWeight = FontWeight.Black,
            fontSize = (57 * s).sp,
            lineHeight = (64 * s).sp,
            letterSpacing = (-0.25).sp
        ),
        displayMedium = TextStyle(
            fontFamily = TitleFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = (45 * s).sp,
            lineHeight = (52 * s).sp,
            letterSpacing = 0.sp
        ),
        displaySmall = TextStyle(
            fontFamily = TitleFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (36 * s).sp,
            lineHeight = (44 * s).sp,
            letterSpacing = 0.sp
        ),
        headlineLarge = TextStyle(
            fontFamily = TitleFontFamily,
            fontWeight = FontWeight.Black,
            fontSize = (32 * s).sp,
            lineHeight = (40 * s).sp,
            letterSpacing = 0.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = TitleFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = (28 * s).sp,
            lineHeight = (36 * s).sp,
            letterSpacing = 0.sp
        ),
        headlineSmall = TextStyle(
            fontFamily = TitleFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (24 * s).sp,
            lineHeight = (32 * s).sp,
            letterSpacing = 0.sp
        ),
        titleLarge = TextStyle(
            fontFamily = TitleFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (22 * s).sp,
            lineHeight = (28 * s).sp,
            letterSpacing = 0.sp
        ),
        titleMedium = TextStyle(
            fontFamily = TitleFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (16 * s).sp,
            lineHeight = (24 * s).sp,
            letterSpacing = 0.15.sp
        ),
        titleSmall = TextStyle(
            fontFamily = TitleFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (14 * s).sp,
            lineHeight = (20 * s).sp,
            letterSpacing = 0.1.sp
        ),
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
        labelLarge = TextStyle(
            fontFamily = TitleFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (14 * s).sp,
            lineHeight = (20 * s).sp,
            letterSpacing = 0.1.sp
        ),
        labelMedium = TextStyle(
            fontFamily = TitleFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (12 * s).sp,
            lineHeight = (16 * s).sp,
            letterSpacing = 0.5.sp
        ),
        labelSmall = TextStyle(
            fontFamily = TitleFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (11 * s).sp,
            lineHeight = (16 * s).sp,
            letterSpacing = 0.5.sp
        )
    )
}

val Typography = createTypography(1.0f)
