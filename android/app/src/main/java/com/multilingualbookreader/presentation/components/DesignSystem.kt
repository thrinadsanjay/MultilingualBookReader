package com.multilingualbookreader.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.multilingualbookreader.R
import com.multilingualbookreader.presentation.theme.LocalBrand
import com.multilingualbookreader.presentation.theme.LocalDimens

@Composable
fun BookReaderCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val brand = LocalBrand.current
    val dimens = LocalDimens.current
    val shape = RoundedCornerShape(dimens.cardRadius)
    Column(
        modifier
            .fillMaxWidth()
            .shadow(2.dp, shape, clip = false)
            .clip(shape)
            .background(brand.card)
            .border(1.dp, brand.border, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(dimens.cardPad),
        content = content,
    )
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val brand = LocalBrand.current
    val dimens = LocalDimens.current
    val shape = RoundedCornerShape(dimens.buttonRadius)
    val container = if (enabled) brand.accent else brand.surfaceSecondary
    val content = if (enabled) brand.onAccent else brand.textSecondary
    Row(
        modifier
            .fillMaxWidth()
            .height(dimens.buttonHeight)
            .clip(shape)
            .background(container)
            .then(if (enabled) Modifier else Modifier.border(1.dp, brand.border, shape))
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = text },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = content, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = content, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val brand = LocalBrand.current
    val dimens = LocalDimens.current
    val shape = RoundedCornerShape(dimens.buttonRadius)
    Row(
        modifier
            .fillMaxWidth()
            .height(dimens.buttonHeight)
            .clip(shape)
            .background(brand.surfaceSecondary)
            .border(1.dp, brand.border, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = text },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = brand.textPrimary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = brand.textPrimary, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SectionHeader(
    title: String,
    action: String? = null,
    onAction: (() -> Unit)? = null,
    actionColor: Color? = null,
) {
    val brand = LocalBrand.current
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = brand.textPrimary)
        if (action != null && onAction != null) {
            Text(
                action,
                color = actionColor ?: brand.accent,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onAction)
                    .padding(4.dp)
                    .semantics { contentDescription = action },
            )
        }
    }
}

@Composable
fun StatusPill(online: Boolean) {
    val brand = LocalBrand.current
    val color = if (online) brand.teal else brand.danger
    val label = if (online) "Online" else "Offline"
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .semantics { contentDescription = label },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(color))
        Text(label, color = color, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun ReaderProgressBar(percent: Int, modifier: Modifier = Modifier) {
    val brand = LocalBrand.current
    LinearProgressIndicator(
        progress = { (percent / 100f).coerceIn(0f, 1f) },
        modifier = modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
        color = brand.accent,
        trackColor = brand.border,
        gapSize = 0.dp,
        drawStopIndicator = {},
    )
}

@Composable
fun FilterChipItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val brand = LocalBrand.current
    val dimens = LocalDimens.current
    val shape = RoundedCornerShape(dimens.chipRadius)
    Text(
        text = label,
        color = if (selected) brand.onAccent else brand.textSecondary,
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier
            .clip(shape)
            .background(if (selected) brand.accent else brand.card)
            .border(1.dp, if (selected) brand.accent else brand.border, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .semantics { contentDescription = label },
    )
}

@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = true,
) {
    val brand = LocalBrand.current
    val shape = RoundedCornerShape(20.dp)
    Text(
        text = text,
        color = if (filled) brand.onAccent else brand.textPrimary,
        style = MaterialTheme.typography.labelMedium,
        modifier = modifier
            .clip(shape)
            .background(if (filled) brand.accent else brand.surfaceSecondary)
            .then(if (filled) Modifier else Modifier.border(1.dp, brand.border, shape))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .semantics { contentDescription = text },
    )
}

@Composable
fun IconTile(
    icon: ImageVector,
    tint: Color,
    container: Color,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    radius: Dp = 12.dp,
) {
    Box(
        modifier.size(size).clip(RoundedCornerShape(radius)).background(container),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}

/** The Svara launcher mark on its dark tile, so the brand reads the same inside the app. */
@Composable
fun BrandMark(modifier: Modifier = Modifier, size: Dp = 34.dp) {
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(size / 3.2f))
            .background(colorResource(R.color.launcher_bg)),
        contentAlignment = Alignment.Center,
    ) {
        // Launchers show the middle 72 of the icon's 108 viewport; scaling by the same 1.5 keeps
        // the in-app mark identical to the one on the home screen.
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.size(size * 1.5f),
        )
    }
}

/** Brand mark plus wordmark, used in the Home header and About. */
@Composable
fun BrandLockup(
    modifier: Modifier = Modifier,
    markSize: Dp = 34.dp,
    showTagline: Boolean = false,
) {
    val brand = LocalBrand.current
    Row(
        modifier.semantics { contentDescription = "Svara" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BrandMark(size = markSize)
        Column {
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                color = brand.textPrimary,
            )
            if (showTagline) {
                Text(
                    stringResource(R.string.app_tagline),
                    style = MaterialTheme.typography.labelMedium,
                    color = brand.textSecondary,
                )
            }
        }
    }
}

@Composable
fun ReaderTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
) {
    val brand = LocalBrand.current
    Row(
        Modifier
            .fillMaxWidth()
            .background(brand.background)
            .heightIn(min = 56.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = brand.textPrimary)
            }
        }
        Text(title, style = MaterialTheme.typography.titleLarge, color = brand.textPrimary)
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    val brand = LocalBrand.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = brand.textPrimary, modifier = Modifier.padding(horizontal = 4.dp))
        BookReaderCard { content() }
    }
}

@Composable
fun SettingsRow(
    label: String,
    icon: ImageVector,
    value: String? = null,
    // Declared last so a trailing lambda at a call site is the click handler, never the slot.
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val brand = LocalBrand.current
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = LocalDimens.current.touch)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = brand.textSecondary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = brand.textPrimary, modifier = Modifier.weight(1f))
        if (trailing != null) {
            trailing()
        } else {
            if (value != null) {
                Text(value, style = MaterialTheme.typography.bodyMedium, color = brand.accent)
                Spacer(Modifier.width(4.dp))
            }
            if (onClick != null) {
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = brand.textSecondary)
            }
        }
    }
}

@Composable
fun ToggleRow(
    label: String,
    icon: ImageVector,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
) {
    val brand = LocalBrand.current
    SettingsRow(label = label, icon = icon, trailing = {
        Switch(
            checked = checked,
            onCheckedChange = onChecked,
            colors = SwitchDefaults.colors(
                checkedTrackColor = brand.accent,
                checkedThumbColor = brand.onAccent,
                checkedBorderColor = brand.accent,
                uncheckedTrackColor = brand.surfaceSecondary,
                uncheckedThumbColor = brand.textSecondary,
                uncheckedBorderColor = brand.border,
            ),
        )
    })
}

@Composable
fun EmptyState(
    title: String,
    message: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    primary: String? = null,
    onPrimary: (() -> Unit)? = null,
    secondary: String? = null,
    onSecondary: (() -> Unit)? = null,
) {
    val brand = LocalBrand.current
    Column(
        modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(112.dp).clip(CircleShape).background(brand.surfaceSecondary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = brand.accent, modifier = Modifier.size(52.dp))
        }
        Text(title, style = MaterialTheme.typography.titleLarge, color = brand.textPrimary)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = brand.textSecondary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (primary != null && onPrimary != null) {
                PrimaryButton(primary, onPrimary, modifier = Modifier.weight(1f))
            }
            if (secondary != null && onSecondary != null) {
                SecondaryButton(secondary, onSecondary, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun ScreenHeader(title: String, subtitle: String? = null) {
    val brand = LocalBrand.current
    Column(Modifier.padding(bottom = 4.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, color = brand.textPrimary)
        if (subtitle != null) {
            Spacer(Modifier.height(6.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = brand.textSecondary)
        }
    }
}
