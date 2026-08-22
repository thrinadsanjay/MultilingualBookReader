package com.multilingualbookreader.presentation.update

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.multilingualbookreader.presentation.components.GradientButton
import com.multilingualbookreader.presentation.components.GradientChip
import com.multilingualbookreader.presentation.theme.LocalBrand
import com.multilingualbookreader.update.AppUpdateManager
import com.multilingualbookreader.update.UpdateUiState

@Composable
fun UpdateSection(
    state: UpdateUiState,
    manager: AppUpdateManager,
    onCheck: () -> Unit,
    onDownload: () -> Unit,
) {
    val context = LocalContext.current
    val brand = LocalBrand.current
    val directInstall = manager.canInstallFromThisApp()
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("App Updates", style = MaterialTheme.typography.titleLarge)
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(brand.card)
                .border(1.dp, brand.cardBorder, RoundedCornerShape(22.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(brand.purple.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.RocketLaunch, contentDescription = null, tint = brand.purple)
                }
                Column(Modifier.weight(1f)) {
                    Text("Installed: v${state.currentVersion}", style = MaterialTheme.typography.titleMedium)
                    Text(
                        state.message
                            ?: if (directInstall) {
                                "Check for a newer test build."
                            } else {
                                "If install is blocked, open the update in Chrome or Files."
                            },
                        style = MaterialTheme.typography.bodySmall,
                        color = brand.muted,
                    )
                }
                GradientChip(
                    text = if (state.checking) "Checking…" else "Check for update",
                    brush = brand.purpleGradient,
                    onClick = onCheck,
                    enabled = !state.checking && !state.downloading,
                )
            }
            if (state.downloading) {
                LinearProgressIndicator(
                    progress = { state.progressPercent / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = brand.orange,
                    trackColor = brand.cardBorder,
                )
            }
            state.available?.let { update ->
                GradientButton(
                    text = "Download ${update.versionName}",
                    brush = brand.orangeGradient,
                    icon = Icons.Outlined.Download,
                    onClick = {
                        if (directInstall) {
                            onDownload()
                        } else {
                            manager.browserDownloadIntent(update.apkUrl)?.let { context.startActivity(it) }
                        }
                    },
                    enabled = !state.downloading,
                    contentColor = Color(0xFFFFF3E0),
                )
            }
            if (directInstall) {
                state.downloadedFile?.let { file ->
                    GradientButton(
                        text = "Install update",
                        brush = brand.orangeGradient,
                        onClick = { context.startActivity(manager.installIntent(file)) },
                    )
                }
            }
        }
    }
}
