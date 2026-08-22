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
    val scanSurface: Color,
    val onScanSurface: Color,
    val scanIcon: Color,
    val importSurface: Color,
    val onImportSurface: Color,
    val importIcon: Color,
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
    background = Color(0xFF0E0E11),
    surfaceSecondary = Color(0xFF17171B),
    card = Color(0xFF1A1A1F),
    elevated = Color(0xFF212127),
    textPrimary = Color(0xFFF3F1ED),
    textSecondary = Color(0xFF9A968F),
    accent = Color(0xFFE8A87C),
    onAccent = Color(0xFF33200F),
    teal = Color(0xFF6FBFA8),
    border = Color(0xFF2A2A31),
    danger = Color(0xFFE07A70),
    navBar = Color(0xFF141418),
    scanSurface = Color(0xFFE8A87C),
    onScanSurface = Color(0xFF33200F),
    scanIcon = Color(0xFF33200F),
    importSurface = Color(0xFF1A1A1F),
    onImportSurface = Color(0xFFF3F1ED),
    importIcon = Color(0xFFBFBBB4),
)

fun lightBrand() = BrandColors(
    background = Color(0xFFFCFAF7),
    surfaceSecondary = Color(0xFFF3EFE9),
    card = Color(0xFFFFFFFF),
    elevated = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF241F1A),
    textSecondary = Color(0xFF7A7269),
    accent = Color(0xFFE29A6C),
    onAccent = Color(0xFF33200F),
    teal = Color(0xFF3E8F7C),
    border = Color(0xFFEBE3D9),
    danger = Color(0xFFC2402F),
    navBar = Color(0xFFFFFFFF),
    scanSurface = Color(0xFFF8E2CE),
    onScanSurface = Color(0xFF3A2A1B),
    scanIcon = Color(0xFF9A5B2B),
    importSurface = Color(0xFFE2EFE8),
    onImportSurface = Color(0xFF23342D),
    importIcon = Color(0xFF3E8F7C),
)
