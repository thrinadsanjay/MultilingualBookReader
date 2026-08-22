package com.multilingualbookreader.presentation.update

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
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
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("App updates", style = MaterialTheme.typography.titleMedium)
        Text("Installed: ${state.currentVersion}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            "New test builds install from here. Android will ask you to allow installs from Book Reader once.",
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
        if (state.available != null && state.downloadedFile == null) {
            LargeButton(
                text = "Download ${state.available.versionName}",
                onClick = onDownload,
                enabled = !state.downloading,
            )
        }
        state.downloadedFile?.let { file ->
            LargeButton(
                text = "Install update",
                onClick = {
                    if (!context.packageManager.canRequestPackageInstallsSafe()) {
                        context.findActivity()?.startActivity(manager.installPermissionIntent())
                    } else {
                        context.startActivity(manager.installIntent(file))
                    }
                },
            )
        }
    }
}

private fun PackageManager.canRequestPackageInstallsSafe(): Boolean {
    return runCatching { canRequestPackageInstalls() }.getOrDefault(true)
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
