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
    primary = Color(0xFFE29A6C),
    onPrimary = Color(0xFF33200F),
    primaryContainer = Color(0xFFF8E2CE),
    onPrimaryContainer = Color(0xFF3A2A1B),
    secondary = Color(0xFF3E8F7C),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2EFE8),
    onSecondaryContainer = Color(0xFF23342D),
    background = Color(0xFFFCFAF7),
    onBackground = Color(0xFF241F1A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF241F1A),
    surfaceVariant = Color(0xFFF3EFE9),
    onSurfaceVariant = Color(0xFF7A7269),
    surfaceContainer = Color(0xFFF3EFE9),
    surfaceContainerHigh = Color(0xFFFFFFFF),
    surfaceTint = Color.Transparent,
    outline = Color(0xFFEBE3D9),
    outlineVariant = Color(0xFFF1EAE1),
    error = Color(0xFFC2402F),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFE8A87C),
    onPrimary = Color(0xFF33200F),
    primaryContainer = Color(0xFF4A2E17),
    onPrimaryContainer = Color(0xFFFFE1C9),
    secondary = Color(0xFF6FBFA8),
    onSecondary = Color(0xFF06231D),
    secondaryContainer = Color(0xFF17171B),
    onSecondaryContainer = Color(0xFFD7F6EC),
    background = Color(0xFF0E0E11),
    onBackground = Color(0xFFF3F1ED),
    surface = Color(0xFF1A1A1F),
    onSurface = Color(0xFFF3F1ED),
    surfaceVariant = Color(0xFF17171B),
    onSurfaceVariant = Color(0xFF9A968F),
    surfaceContainer = Color(0xFF141418),
    surfaceContainerHigh = Color(0xFF212127),
    surfaceTint = Color.Transparent,
    outline = Color(0xFF2A2A31),
    outlineVariant = Color(0xFF24242A),
    error = Color(0xFFE07A70),
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
