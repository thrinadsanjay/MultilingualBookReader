package com.multilingualbookreader.presentation.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

data class BrandColors(
    val orange: Color,
    val orangeSoft: Color,
    val teal: Color,
    val tealDeep: Color,
    val purple: Color,
    val purpleDeep: Color,
    val card: Color,
    val cardBorder: Color,
    val online: Color,
    val muted: Color,
    val navBar: Color,
    val scanGradient: Brush,
    val importBackground: Color,
    val purpleGradient: Brush,
    val orangeGradient: Brush,
    val coverGradient: Brush,
    val voiceIconGradient: Brush,
)

val LocalBrand = staticCompositionLocalOf { darkBrand() }

fun darkBrand() = BrandColors(
    orange = Color(0xFFFF8A3C),
    orangeSoft = Color(0xFFFFC08A),
    teal = Color(0xFF2DD4C3),
    tealDeep = Color(0xFF163A36),
    purple = Color(0xFF7B6CFF),
    purpleDeep = Color(0xFF3A45E8),
    card = Color(0xFF1C1C21),
    cardBorder = Color(0xFF2A2A32),
    online = Color(0xFF22C55E),
    muted = Color(0xFF9A9AA3),
    navBar = Color(0xFF121216),
    scanGradient = Brush.linearGradient(listOf(Color(0xFFFF7A2E), Color(0xFFFFB067))),
    importBackground = Color(0xFF172422),
    purpleGradient = Brush.horizontalGradient(listOf(Color(0xFF7B6CFF), Color(0xFF3D4BFF))),
    orangeGradient = Brush.horizontalGradient(listOf(Color(0xFFB56A38), Color(0xFFE08A3C))),
    coverGradient = Brush.linearGradient(listOf(Color(0xFFFF8A5B), Color(0xFF6B4C9A))),
    voiceIconGradient = Brush.linearGradient(listOf(Color(0xFF2B3A8C), Color(0xFF1A1F4A))),
)

fun lightBrand() = BrandColors(
    orange = Color(0xFFE56720),
    orangeSoft = Color(0xFF8C4A1F),
    teal = Color(0xFF0F766E),
    tealDeep = Color(0xFFD5EDE8),
    purple = Color(0xFF5B4DDB),
    purpleDeep = Color(0xFF312E81),
    card = Color(0xFFFFF9F2),
    cardBorder = Color(0xFFE7D8C6),
    online = Color(0xFF15803D),
    muted = Color(0xFF6B6258),
    navBar = Color(0xFFFFF7EE),
    scanGradient = Brush.linearGradient(listOf(Color(0xFFE56720), Color(0xFFF4A15A))),
    importBackground = Color(0xFFE7F3EF),
    purpleGradient = Brush.horizontalGradient(listOf(Color(0xFF6D5EF7), Color(0xFF4338CA))),
    orangeGradient = Brush.horizontalGradient(listOf(Color(0xFFC45C26), Color(0xFFE08A3C))),
    coverGradient = Brush.linearGradient(listOf(Color(0xFFF4A15A), Color(0xFF7C5C9A))),
    voiceIconGradient = Brush.linearGradient(listOf(Color(0xFF4C5BD4), Color(0xFF2A3178))),
)
