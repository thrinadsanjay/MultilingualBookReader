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
import com.multilingualbookreader.presentation.components.BookReaderCard
import com.multilingualbookreader.presentation.components.PrimaryButton
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
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BookReaderCard {
            Text("Installed: v${state.currentVersion}", style = MaterialTheme.typography.titleMedium, color = brand.textPrimary)
            Text(
                state.message ?: if (state.available == null) "You are up to date." else "A newer test build is ready.",
                style = MaterialTheme.typography.bodySmall,
                color = brand.textSecondary,
            )
            if (state.downloading) {
                LinearProgressIndicator(progress = { state.progressPercent / 100f }, modifier = Modifier, color = brand.accent, trackColor = brand.border)
            }
        }
        PrimaryButton(if (state.checking) "Checking…" else "Check for update", onCheck, enabled = !state.checking && !state.downloading)
        state.available?.let { update ->
            Text("Latest: ${update.versionName}", color = brand.textSecondary, style = MaterialTheme.typography.bodyMedium)
            if (update.notes.isNotBlank()) {
                Text(update.notes.take(280), color = brand.textSecondary, style = MaterialTheme.typography.bodySmall)
            }
            PrimaryButton(
                text = if (directInstall) "Download ${update.versionName}" else "Open ${update.versionName} in browser",
                onClick = {
                    if (directInstall) onDownload() else manager.browserDownloadIntent(update.apkUrl)?.let { context.startActivity(it) }
                },
                enabled = !state.downloading,
            )
        }
        if (directInstall) {
            state.downloadedFile?.let { file ->
                PrimaryButton("Install update", { context.startActivity(manager.installIntent(file)) })
            }
        }
    }
}
