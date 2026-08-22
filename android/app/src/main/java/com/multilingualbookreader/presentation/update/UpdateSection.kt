package com.multilingualbookreader.presentation.update

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.InstallMobile
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Shop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.multilingualbookreader.presentation.components.BookReaderCard
import com.multilingualbookreader.presentation.theme.LocalBrand
import com.multilingualbookreader.presentation.theme.LocalDimens
import com.multilingualbookreader.update.InstallChannel
import com.multilingualbookreader.update.UpdateAction
import com.multilingualbookreader.update.UpdateActionIcon
import com.multilingualbookreader.update.UpdateActionKind
import com.multilingualbookreader.update.UpdateCopy
import com.multilingualbookreader.update.UpdatePhase
import com.multilingualbookreader.update.UpdateUiState

/**
 * The whole update experience: a short status card and exactly one primary action whose label,
 * icon, progress, and behaviour come from the current phase.
 */
@Composable
fun UpdateSection(
    state: UpdateUiState,
    channel: InstallChannel,
    onAction: (UpdateActionKind) -> Unit,
) {
    val brand = LocalBrand.current
    val phase = state.phase
    val headline = UpdateCopy.headline(phase)
    val notes = UpdateCopy.releaseNotes(phase)
    val primary = UpdateCopy.primaryAction(phase, channel)
    val secondary = UpdateCopy.secondaryAction(phase, channel)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BookReaderCard {
            Text("App updates", style = MaterialTheme.typography.titleMedium, color = brand.textPrimary)
            Spacer(Modifier.height(10.dp))
            Text("Installed", style = MaterialTheme.typography.bodySmall, color = brand.textSecondary)
            Text(state.installedLabel, style = MaterialTheme.typography.titleMedium, color = brand.textPrimary)

            if (headline != null) {
                Spacer(Modifier.height(14.dp))
                Text(
                    headline.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (headline.isError) brand.danger else brand.textPrimary,
                )
                headline.detail?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = brand.textSecondary)
                }
            }

            if (notes.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text("What's new", style = MaterialTheme.typography.bodySmall, color = brand.textSecondary)
                Spacer(Modifier.height(4.dp))
                notes.forEach { line ->
                    Row(Modifier.padding(top = 2.dp)) {
                        Text("•", style = MaterialTheme.typography.bodyMedium, color = brand.accent)
                        Spacer(Modifier.width(8.dp))
                        Text(line, style = MaterialTheme.typography.bodyMedium, color = brand.textSecondary)
                    }
                }
            }

            if (phase is UpdatePhase.Downloading) {
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { (phase.percent / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                    color = brand.accent,
                    trackColor = brand.border,
                    gapSize = 0.dp,
                    drawStopIndicator = {},
                )
            }
        }

        if (channel == InstallChannel.BLOCKED) {
            BookReaderCard {
                Text("Advanced Protection is on", style = MaterialTheme.typography.titleMedium, color = brand.textPrimary)
                Text(
                    "Android is blocking installs from every app on this phone, so updates have to come from Google Play.",
                    style = MaterialTheme.typography.bodySmall,
                    color = brand.textSecondary,
                )
            }
        }

        UpdateActionButton(action = primary, onClick = { onAction(primary.kind) })
        secondary?.let { action ->
            UpdateActionButton(action = action, onClick = { onAction(action.kind) }, primary = false)
        }
    }
}

@Composable
private fun UpdateActionButton(
    action: UpdateAction,
    onClick: () -> Unit,
    primary: Boolean = true,
) {
    val brand = LocalBrand.current
    val dimens = LocalDimens.current
    val shape = RoundedCornerShape(dimens.buttonRadius)
    val container = when {
        !primary -> brand.surfaceSecondary
        action.enabled -> brand.accent
        else -> brand.accent.copy(alpha = 0.55f)
    }
    val content = when {
        !primary -> brand.textPrimary
        else -> brand.onAccent
    }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = dimens.buttonHeight)
            .clip(shape)
            .background(container)
            .then(if (primary) Modifier else Modifier.border(1.dp, brand.border, shape))
            .clickable(enabled = action.enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .semantics { contentDescription = action.label },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (action.busy) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = content,
                strokeWidth = 2.dp,
            )
            Spacer(Modifier.width(10.dp))
        } else {
            action.icon.vector()?.let {
                Icon(it, null, tint = content, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
            }
        }
        Text(
            action.label,
            color = content,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

private fun UpdateActionIcon.vector(): ImageVector? = when (this) {
    UpdateActionIcon.REFRESH -> Icons.Outlined.Refresh
    UpdateActionIcon.DOWNLOAD -> Icons.Outlined.Download
    UpdateActionIcon.INSTALL -> Icons.Outlined.InstallMobile
    UpdateActionIcon.PLAY -> Icons.Outlined.Shop
    UpdateActionIcon.NONE -> null
}
