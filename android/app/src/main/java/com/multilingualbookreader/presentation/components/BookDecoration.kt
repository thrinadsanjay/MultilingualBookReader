package com.multilingualbookreader.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun OpenBookDecoration(modifier: Modifier = Modifier, color: Color = Color.White.copy(alpha = 0.08f)) {
    Canvas(modifier.size(140.dp, 110.dp)) {
        val page = Size(width = size.width * 0.38f, height = size.height * 0.62f)
        val left = Offset(size.width * 0.12f, size.height * 0.22f)
        val right = Offset(size.width * 0.50f, size.height * 0.18f)
        drawRoundRect(color, left, page, CornerRadius(12f, 12f))
        drawRoundRect(color.copy(alpha = color.alpha * 1.2f), right, page, CornerRadius(12f, 12f))
        drawRoundRect(
            color.copy(alpha = color.alpha * 0.5f),
            Offset(size.width * 0.46f, size.height * 0.20f),
            Size(size.width * 0.08f, size.height * 0.64f),
            CornerRadius(8f, 8f),
        )
    }
}
