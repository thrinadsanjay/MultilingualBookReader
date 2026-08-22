package com.multilingualbookreader.presentation.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Shop
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.multilingualbookreader.presentation.components.BookReaderCard
import com.multilingualbookreader.presentation.components.PrimaryButton
import com.multilingualbookreader.presentation.components.SecondaryButton
import com.multilingualbookreader.presentation.theme.LocalBrand
import com.multilingualbookreader.update.AppUpdateManager
import com.multilingualbookreader.update.InstallChannel
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
    val channel = remember(state.checking, state.available, state.downloadedFile) { manager.installChannel() }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BookReaderCard {
            Text("Installed: v${state.currentVersion}", style = MaterialTheme.typography.titleMedium, color = brand.textPrimary)
            Text(
                state.message ?: if (state.available == null) "You are up to date." else "A newer test build is ready.",
                style = MaterialTheme.typography.bodySmall,
                color = brand.textSecondary,
            )
            if (state.downloading) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(progress = { state.progressPercent / 100f }, modifier = Modifier, color = brand.accent, trackColor = brand.border)
            }
        }

        when (channel) {
            InstallChannel.PLAY -> {
                BookReaderCard {
                    Text("Updates come from Google Play", style = MaterialTheme.typography.titleMedium, color = brand.textPrimary)
                    Text(
                        "Play installs new versions in the background, so you can leave Advanced Protection on.",
                        style = MaterialTheme.typography.bodySmall,
                        color = brand.textSecondary,
                    )
                }
                PrimaryButton("Open Google Play", { manager.openPlayStore() }, icon = Icons.Outlined.Shop)
            }

            InstallChannel.BLOCKED -> {
                BookReaderCard {
                    Text("Advanced Protection is on", style = MaterialTheme.typography.titleMedium, color = brand.textPrimary)
                    Text(
                        "Android is blocking APK installs from every app on this phone, including Chrome, Files, and adb. " +
                            "Install this app from Google Play to keep getting updates without turning protection off.",
                        style = MaterialTheme.typography.bodySmall,
                        color = brand.textSecondary,
                    )
                }
                PrimaryButton("Open Google Play", { manager.openPlayStore() }, icon = Icons.Outlined.Shop)
                SecondaryButton(if (state.checking) "Checking…" else "Check for update", onCheck, enabled = !state.checking)
                state.available?.let { update ->
                    Text(
                        "Latest test build: ${update.versionName}. Downloading it here would fail, so it is not offered.",
                        color = brand.textSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            InstallChannel.NEEDS_PERMISSION -> {
                BookReaderCard {
                    Text("Allow installs once", style = MaterialTheme.typography.titleMedium, color = brand.textPrimary)
                    Text(
                        "Android needs permission to let Book Reader install its own update.",
                        style = MaterialTheme.typography.bodySmall,
                        color = brand.textSecondary,
                    )
                }
                PrimaryButton("Allow installs from Book Reader", { context.startActivity(manager.installPermissionIntent()) })
                SecondaryButton(if (state.checking) "Checking…" else "Check for update", onCheck, enabled = !state.checking)
                state.available?.let { update ->
                    PrimaryButton(
                        text = "Open ${update.versionName} in browser",
                        onClick = { manager.browserDownloadIntent(update.apkUrl)?.let { context.startActivity(it) } },
                    )
                }
            }

            InstallChannel.DIRECT -> {
                PrimaryButton(
                    if (state.checking) "Checking…" else "Check for update",
                    onCheck,
                    enabled = !state.checking && !state.downloading,
                )
                state.available?.let { update ->
                    Text("Latest: ${update.versionName}", color = brand.textSecondary, style = MaterialTheme.typography.bodyMedium)
                    if (update.notes.isNotBlank()) {
                        Text(update.notes.take(280), color = brand.textSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                    PrimaryButton("Download ${update.versionName}", onDownload, enabled = !state.downloading)
                }
                state.downloadedFile?.let { file ->
                    PrimaryButton("Install update", { context.startActivity(manager.installIntent(file)) })
                }
            }
        }
    }
}
