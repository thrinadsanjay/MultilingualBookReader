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
    primary = Color(0xFFFF8A3C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF4A2A14),
    onPrimaryContainer = Color(0xFFFFE0C8),
    secondary = Color(0xFF2DD4C3),
    onSecondary = Color(0xFF04221E),
    secondaryContainer = Color(0xFF163A36),
    onSecondaryContainer = Color(0xFFD7F6F1),
    tertiary = Color(0xFF7B6CFF),
    background = Color(0xFF0E0E12),
    onBackground = Color(0xFFF4F4F7),
    surface = Color(0xFF1C1C21),
    onSurface = Color(0xFFF4F4F7),
    surfaceVariant = Color(0xFF2A2A32),
    onSurfaceVariant = Color(0xFF9A9AA3),
    surfaceContainer = Color(0xFF121216),
    surfaceContainerHigh = Color(0xFF1C1C21),
    surfaceTint = Color.Transparent,
    outline = Color(0xFF3A3A44),
    outlineVariant = Color(0xFF2A2A32),
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
    val brand = if (darkTheme && !highContrast) darkBrand() else if (!darkTheme && !highContrast) lightBrand() else if (darkTheme) darkBrand() else lightBrand()
    CompositionLocalProvider(
        LocalControlSizes provides ControlSizes(),
        LocalBrand provides brand,
    ) {
        MaterialTheme(
            colorScheme = colors,
            typography = bookTypography(fontScale),
            shapes = MaterialTheme.shapes.copy(
                extraSmall = RoundedCornerShape(12.dp),
                small = RoundedCornerShape(16.dp),
                medium = RoundedCornerShape(22.dp),
                large = RoundedCornerShape(28.dp),
            ),
            content = content,
        )
    }
}

private fun bookTypography(scale: Float): Typography {
    val family = FontFamily.SansSerif
    return Typography(
        displaySmall = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold, fontSize = (28 * scale).sp, lineHeight = (34 * scale).sp),
        headlineMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold, fontSize = (18 * scale).sp, lineHeight = (24 * scale).sp),
        titleLarge = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold, fontSize = (17 * scale).sp, lineHeight = (22 * scale).sp),
        titleMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold, fontSize = (15 * scale).sp, lineHeight = (20 * scale).sp),
        bodyLarge = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = (15 * scale).sp, lineHeight = (22 * scale).sp),
        bodyMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = (14 * scale).sp, lineHeight = (20 * scale).sp),
        bodySmall = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = (12 * scale).sp, lineHeight = (16 * scale).sp),
        labelLarge = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium, fontSize = (14 * scale).sp, lineHeight = (18 * scale).sp),
        labelMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium, fontSize = (12 * scale).sp, lineHeight = (16 * scale).sp),
    )
}
