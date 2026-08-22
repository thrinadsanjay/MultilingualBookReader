package com.multilingualbookreader.presentation.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.multilingualbookreader.presentation.components.LargeButton
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
    val directInstall = manager.canInstallFromThisApp()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("App updates", style = MaterialTheme.typography.titleMedium)
        Text("Installed: ${state.currentVersion}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            if (directInstall) {
                "New test builds can install from here, or from Chrome / Files."
            } else {
                "This phone blocks installs from Book Reader (Advanced Protection or a work policy). Leave device security on. Open the update in Chrome or Files instead — the same way the first APK was installed."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        state.message?.let { Text(it) }
        if (state.downloading) {
            LinearProgressIndicator(progress = { state.progressPercent / 100f }, modifier = Modifier)
            Text("Downloading ${state.progressPercent}%")
        }
        LargeButton(
            text = if (state.checking) "Checking…" else "Check for update",
            onClick = onCheck,
            tonal = true,
            enabled = !state.checking && !state.downloading,
        )
        state.available?.let { update ->
            LargeButton(
                text = "Open ${update.versionName} in browser",
                onClick = {
                    manager.browserDownloadIntent(update.apkUrl)?.let { intent ->
                        context.startActivity(intent)
                    }
                },
            )
            if (directInstall && state.downloadedFile == null) {
                LargeButton(
                    text = "Download in the app",
                    onClick = onDownload,
                    tonal = true,
                    enabled = !state.downloading,
                )
            }
        }
        if (directInstall) {
            state.downloadedFile?.let { file ->
                LargeButton(
                    text = "Install update",
                    onClick = { context.startActivity(manager.installIntent(file)) },
                )
            }
        }
    }
}
