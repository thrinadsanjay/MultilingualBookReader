package com.multilingualbookreader.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.multilingualbookreader.presentation.theme.LocalBrand

@Composable
fun StatusBadge(online: Boolean) {
    val brand = LocalBrand.current
    val color = if (online) brand.online else MaterialTheme.colorScheme.error
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(color))
        Text(if (online) "Online" else "Offline", color = color, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun SectionLinkRow(
    title: String,
    action: String,
    onAction: () -> Unit,
    actionColor: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(
            text = action,
            color = actionColor,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onAction)
                .padding(horizontal = 4.dp, vertical = 4.dp)
                .semantics { contentDescription = action },
        )
    }
}

@Composable
fun HeroActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    background: Brush? = null,
    backgroundColor: Color? = null,
    iconTint: Color,
    iconWell: Color,
    subtitleColor: Color,
    arrowWell: Color,
    arrowTint: Color,
    titleColor: Color = Color.White,
) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier
            .height(168.dp)
            .clip(shape)
            .then(
                if (background != null) Modifier.background(background) else Modifier.background(backgroundColor ?: Color.DarkGray),
            )
            .clickable(onClick = onClick)
            .semantics { contentDescription = title }
            .padding(16.dp),
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconWell),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, color = titleColor, style = MaterialTheme.typography.titleLarge)
                Text(subtitle, color = subtitleColor, style = MaterialTheme.typography.bodySmall)
            }
        }
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .size(32.dp)
                .clip(CircleShape)
                .background(arrowWell),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = arrowTint, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun GradientChip(
    text: String,
    brush: Brush,
    onClick: () -> Unit,
    enabled: Boolean = true,
    contentColor: Color = Color.White,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(brush, RoundedCornerShape(20.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .semantics { contentDescription = text },
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = contentColor, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
    }
}

@Composable
fun GradientButton(
    text: String,
    brush: Brush,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    contentColor: Color = Color(0xFFFFF3E0),
) {
    Row(
        modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(brush)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = text },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = contentColor, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold))
    }
}

@Composable
fun VoiceHomeCard(
    name: String,
    detail: String,
    onCreate: () -> Unit,
) {
    val brand = LocalBrand.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(brand.card)
            .border(1.dp, brand.cardBorder, RoundedCornerShape(22.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(brand.voiceIconGradient),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.GraphicEq, contentDescription = null, tint = Color.White)
        }
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleMedium)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = brand.muted)
        }
        GradientChip(text = "Create Voice", brush = brand.purpleGradient, onClick = onCreate)
    }
}

@Composable
fun ContinueListeningCard(
    title: String,
    pageLabel: String,
    progressPercent: Int,
    onPlay: () -> Unit,
) {
    val brand = LocalBrand.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(brand.card)
            .border(1.dp, brand.cardBorder, RoundedCornerShape(22.dp))
            .clickable(onClick = onPlay)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(width = 52.dp, height = 64.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(brand.coverGradient),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(pageLabel, style = MaterialTheme.typography.bodySmall, color = brand.muted)
            LinearProgressIndicator(
                progress = { (progressPercent / 100f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = brand.teal,
                trackColor = brand.cardBorder,
            )
        }
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(brand.teal),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.PlayArrow, contentDescription = "Play", tint = Color(0xFF04221E))
        }
    }
}
