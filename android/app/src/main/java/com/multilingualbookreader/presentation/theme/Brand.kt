package com.multilingualbookreader.presentation.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class BrandColors(
    val background: Color,
    val surfaceSecondary: Color,
    val card: Color,
    val elevated: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val onAccent: Color,
    val teal: Color,
    val border: Color,
    val danger: Color,
    val navBar: Color,
    val importSurface: Color,
)

data class BrandDimens(
    val screen: Dp = 20.dp,
    val cardRadius: Dp = 24.dp,
    val buttonRadius: Dp = 22.dp,
    val chipRadius: Dp = 20.dp,
    val cardPad: Dp = 16.dp,
    val gap: Dp = 16.dp,
    val touch: Dp = 48.dp,
    val buttonHeight: Dp = 52.dp,
    val heroCardHeight: Dp = 168.dp,
)

val LocalBrand = staticCompositionLocalOf { darkBrand() }
val LocalDimens = staticCompositionLocalOf { BrandDimens() }

fun darkBrand() = BrandColors(
    background = Color(0xFF100F0D),
    surfaceSecondary = Color(0xFF1B1916),
    card = Color(0xFF211F1C),
    elevated = Color(0xFF27241F),
    textPrimary = Color(0xFFF4EEE6),
    textSecondary = Color(0xFFA39A90),
    accent = Color(0xFFE8A07A),
    onAccent = Color(0xFF2A1810),
    teal = Color(0xFF6FBFB0),
    border = Color(0xFF322E28),
    danger = Color(0xFFE07070),
    navBar = Color(0xFF161411),
    importSurface = Color(0xFF1B1916),
)

fun lightBrand() = BrandColors(
    background = Color(0xFFF6F1E8),
    surfaceSecondary = Color(0xFFF0EBE2),
    card = Color(0xFFFFFCF8),
    elevated = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF2A241C),
    textSecondary = Color(0xFF6F675E),
    accent = Color(0xFFD4895C),
    onAccent = Color(0xFF2A1810),
    teal = Color(0xFF3E8F82),
    border = Color(0xFFE6DCCD),
    danger = Color(0xFFB42318),
    navBar = Color(0xFFFFFCF8),
    importSurface = Color(0xFFEEF5F2),
)
