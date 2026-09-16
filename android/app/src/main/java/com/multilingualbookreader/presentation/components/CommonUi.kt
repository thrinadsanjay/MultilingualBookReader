package com.multilingualbookreader.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun LargeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tonal: Boolean = false,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    if (tonal) {
        SecondaryButton(text, onClick, modifier, enabled, icon)
    } else {
        PrimaryButton(text, onClick, modifier, enabled, icon)
    }
}

@Composable
fun OnlineBanner(online: Boolean) {
    StatusPill(online)
}
