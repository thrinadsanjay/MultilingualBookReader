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
    primary = Color(0xFFD4895C),
    onPrimary = Color(0xFF2A1810),
    primaryContainer = Color(0xFFF3D4B8),
    onPrimaryContainer = Color(0xFF3D1E08),
    secondary = Color(0xFF3E8F82),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD5E8DE),
    onSecondaryContainer = Color(0xFF13261E),
    background = Color(0xFFF6F1E8),
    onBackground = Color(0xFF2A241C),
    surface = Color(0xFFFFFCF8),
    onSurface = Color(0xFF2A241C),
    surfaceVariant = Color(0xFFF0EBE2),
    onSurfaceVariant = Color(0xFF6F675E),
    surfaceContainer = Color(0xFFF0EBE2),
    surfaceContainerHigh = Color(0xFFFFFCF8),
    surfaceTint = Color.Transparent,
    outline = Color(0xFFE6DCCD),
    outlineVariant = Color(0xFFEDE4D6),
    error = Color(0xFFB42318),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFE8A07A),
    onPrimary = Color(0xFF2A1810),
    primaryContainer = Color(0xFF4A2A14),
    onPrimaryContainer = Color(0xFFFFE0C8),
    secondary = Color(0xFF6FBFB0),
    onSecondary = Color(0xFF04221E),
    secondaryContainer = Color(0xFF1B1916),
    onSecondaryContainer = Color(0xFFD7F6F1),
    background = Color(0xFF100F0D),
    onBackground = Color(0xFFF4EEE6),
    surface = Color(0xFF211F1C),
    onSurface = Color(0xFFF4EEE6),
    surfaceVariant = Color(0xFF1B1916),
    onSurfaceVariant = Color(0xFFA39A90),
    surfaceContainer = Color(0xFF161411),
    surfaceContainerHigh = Color(0xFF27241F),
    surfaceTint = Color.Transparent,
    outline = Color(0xFF322E28),
    outlineVariant = Color(0xFF2A2621),
    error = Color(0xFFE07070),
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
    val largeButtonHeight: Dp = 52.dp,
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
    val brand = if (darkTheme) darkBrand() else lightBrand()
    CompositionLocalProvider(
        LocalControlSizes provides ControlSizes(),
        LocalBrand provides brand,
        LocalDimens provides BrandDimens(),
    ) {
        MaterialTheme(
            colorScheme = colors,
            typography = bookTypography(fontScale),
            shapes = MaterialTheme.shapes.copy(
                extraSmall = RoundedCornerShape(12.dp),
                small = RoundedCornerShape(16.dp),
                medium = RoundedCornerShape(24.dp),
                large = RoundedCornerShape(28.dp),
            ),
            content = content,
        )
    }
}

private fun bookTypography(scale: Float): Typography {
    val family = FontFamily.SansSerif
    return Typography(
        displaySmall = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold, fontSize = (26 * scale).sp, lineHeight = (32 * scale).sp),
        headlineMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold, fontSize = (20 * scale).sp, lineHeight = (26 * scale).sp),
        titleLarge = TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold, fontSize = (17 * scale).sp, lineHeight = (22 * scale).sp),
        titleMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold, fontSize = (15 * scale).sp, lineHeight = (20 * scale).sp),
        bodyLarge = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = (15 * scale).sp, lineHeight = (22 * scale).sp),
        bodyMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = (14 * scale).sp, lineHeight = (20 * scale).sp),
        bodySmall = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = (13 * scale).sp, lineHeight = (18 * scale).sp),
        labelLarge = TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold, fontSize = (15 * scale).sp, lineHeight = (20 * scale).sp),
        labelMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium, fontSize = (12 * scale).sp, lineHeight = (16 * scale).sp),
    )
}
