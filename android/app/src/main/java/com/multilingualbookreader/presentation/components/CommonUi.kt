package com.multilingualbookreader.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.multilingualbookreader.presentation.theme.LocalControlSizes

@Composable
fun LargeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tonal: Boolean = false,
    enabled: Boolean = true,
) {
    val height = LocalControlSizes.current.largeButtonHeight
    val buttonModifier = modifier
        .fillMaxWidth()
        .height(height)
        .semantics { contentDescription = text }
    if (tonal) {
        FilledTonalButton(onClick = onClick, enabled = enabled, modifier = buttonModifier) {
            Text(text, style = MaterialTheme.typography.titleLarge)
        }
    } else {
        Button(onClick = onClick, enabled = enabled, modifier = buttonModifier) {
            Text(text, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
fun OnlineBanner(online: Boolean) {
    val label = if (online) "Online" else "Offline"
    Surface(
        color = if (online) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
fun ScreenHeader(title: String, subtitle: String? = null) {
    Column(Modifier.padding(bottom = 16.dp)) {
        Text(title, style = MaterialTheme.typography.displaySmall)
        if (subtitle != null) {
            Spacer(Modifier.height(8.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
