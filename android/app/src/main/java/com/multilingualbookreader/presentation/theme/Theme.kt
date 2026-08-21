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
    primary = Color(0xFF1B4B8A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E4FF),
    secondary = Color(0xFF8A4B1B),
    background = Color(0xFFF7F4EC),
    onBackground = Color(0xFF1C1B16),
    surface = Color(0xFFFFFBF3),
    onSurface = Color(0xFF1C1B16),
    error = Color(0xFFB3261E),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9DC0FF),
    onPrimary = Color(0xFF0B2A54),
    primaryContainer = Color(0xFF163864),
    secondary = Color(0xFFE0B48A),
    background = Color(0xFF121318),
    onBackground = Color(0xFFE8E6DE),
    surface = Color(0xFF1B1C22),
    onSurface = Color(0xFFE8E6DE),
    error = Color(0xFFFFB4AB),
)

private val HighContrastLight = lightColorScheme(
    primary = Color(0xFF002F6C),
    onPrimary = Color.White,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    error = Color(0xFF9B0000),
)

private val HighContrastDark = darkColorScheme(
    primary = Color(0xFFFFF176),
    onPrimary = Color.Black,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color.Black,
    onSurface = Color.White,
    error = Color(0xFFFF8A80),
)

data class ControlSizes(
    val minTouch: Dp = 64.dp,
    val largeButtonHeight: Dp = 68.dp,
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
    val typography = bookTypography(fontScale)
    CompositionLocalProvider(LocalControlSizes provides ControlSizes()) {
        MaterialTheme(
            colorScheme = colors,
            typography = typography,
            shapes = MaterialTheme.shapes.copy(
                extraSmall = RoundedCornerShape(8.dp),
                small = RoundedCornerShape(12.dp),
                medium = RoundedCornerShape(18.dp),
                large = RoundedCornerShape(24.dp),
            ),
            content = content,
        )
    }
}

private fun bookTypography(scale: Float): Typography {
    val body = (22 * scale).sp
    val title = (30 * scale).sp
    return Typography(
        displaySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = title),
        headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = (26 * scale).sp),
        titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = (24 * scale).sp),
        bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = body, lineHeight = (32 * scale).sp),
        bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = (18 * scale).sp, lineHeight = (26 * scale).sp),
        labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = (18 * scale).sp),
    )
}
