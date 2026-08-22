package com.multilingualbookreader.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = Color(0xFF8C4A1F),
    onPrimary = Color(0xFFFFF8F1),
    primaryContainer = Color(0xFFF3D4B8),
    onPrimaryContainer = Color(0xFF3D1E08),
    secondary = Color(0xFF3F6B5A),
    onSecondary = Color(0xFFF4FFF8),
    secondaryContainer = Color(0xFFD5E8DE),
    onSecondaryContainer = Color(0xFF13261E),
    background = Color(0xFFF6F0E6),
    onBackground = Color(0xFF2B241C),
    surface = Color(0xFFFFF9F1),
    onSurface = Color(0xFF2B241C),
    surfaceVariant = Color(0xFFE8DDD0),
    onSurfaceVariant = Color(0xFF5B5147),
    surfaceContainer = Color(0xFFEFE6D8),
    surfaceContainerHigh = Color(0xFFE7DCCB),
    surfaceTint = Color.Transparent,
    outline = Color(0xFFC9B9A6),
    outlineVariant = Color(0xFFE4D6C4),
    error = Color(0xFFB3261E),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFE6B07A),
    onPrimary = Color(0xFF3A220C),
    primaryContainer = Color(0xFF6B3F1C),
    onPrimaryContainer = Color(0xFFFFE4C6),
    secondary = Color(0xFF9CCBBA),
    onSecondary = Color(0xFF123028),
    secondaryContainer = Color(0xFF2A463C),
    onSecondaryContainer = Color(0xFFD7F0E6),
    background = Color(0xFF161310),
    onBackground = Color(0xFFF3E7D6),
    surface = Color(0xFF211C18),
    onSurface = Color(0xFFF3E7D6),
    surfaceVariant = Color(0xFF3A332C),
    onSurfaceVariant = Color(0xFFD2C4B4),
    surfaceContainer = Color(0xFF261F1A),
    surfaceContainerHigh = Color(0xFF302822),
    surfaceTint = Color.Transparent,
    outline = Color(0xFF6E6358),
    outlineVariant = Color(0xFF3F372F),
    error = Color(0xFFFFB4AB),
)

private val HighContrastLight = lightColorScheme(
    primary = Color(0xFF5A2500),
    onPrimary = Color.White,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    error = Color(0xFF9B0000),
)

private val HighContrastDark = darkColorScheme(
    primary = Color(0xFFFFE082),
    onPrimary = Color.Black,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color.Black,
    onSurface = Color.White,
    error = Color(0xFFFF8A80),
)

data class ControlSizes(
    val minTouch: Dp = 48.dp,
    val largeButtonHeight: Dp = 48.dp,
)

val LocalControlSizes = staticCompositionLocalOf { ControlSizes() }

@Composable
fun BookReaderTheme(
    darkTheme: Boolean,
    highContrast: Boolean,
    fontScale: Float,
    content: @Composable () -> Unit,
) {
    val colors: ColorScheme = when {
        highContrast && darkTheme -> HighContrastDark
        highContrast && !darkTheme -> HighContrastLight
        darkTheme -> DarkColors
        else -> LightColors
    }
    CompositionLocalProvider(LocalControlSizes provides ControlSizes()) {
        MaterialTheme(
            colorScheme = colors,
            typography = bookTypography(fontScale),
            shapes = MaterialTheme.shapes.copy(
                extraSmall = RoundedCornerShape(10.dp),
                small = RoundedCornerShape(12.dp),
                medium = RoundedCornerShape(16.dp),
                large = RoundedCornerShape(20.dp),
            ),
            content = content,
        )
    }
}

private fun bookTypography(scale: Float): Typography {
    val family = FontFamily.SansSerif
    return Typography(
        displaySmall = TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold, fontSize = (22 * scale).sp, lineHeight = (28 * scale).sp),
        headlineMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold, fontSize = (18 * scale).sp, lineHeight = (24 * scale).sp),
        titleLarge = TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold, fontSize = (17 * scale).sp, lineHeight = (22 * scale).sp),
        titleMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium, fontSize = (15 * scale).sp, lineHeight = (20 * scale).sp),
        bodyLarge = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = (15 * scale).sp, lineHeight = (22 * scale).sp),
        bodyMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = (14 * scale).sp, lineHeight = (20 * scale).sp),
        bodySmall = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = (12 * scale).sp, lineHeight = (16 * scale).sp),
        labelLarge = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium, fontSize = (14 * scale).sp, lineHeight = (18 * scale).sp),
        labelMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium, fontSize = (12 * scale).sp, lineHeight = (16 * scale).sp),
    )
}
