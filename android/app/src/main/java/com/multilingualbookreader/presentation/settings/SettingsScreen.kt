package com.multilingualbookreader.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.multilingualbookreader.domain.model.OcrRoute
import com.multilingualbookreader.domain.model.ThemeMode
import com.multilingualbookreader.presentation.components.LargeButton
import com.multilingualbookreader.presentation.components.ScreenHeader
import com.multilingualbookreader.presentation.components.SettingRow
import com.multilingualbookreader.presentation.update.UpdateSection
import com.multilingualbookreader.update.AppUpdateManager
import com.multilingualbookreader.update.UpdateUiState

@Composable
fun SettingsRoute(
    onPrivacy: () -> Unit,
    onVoiceTest: () -> Unit,
    onBack: () -> Unit,
    showBack: Boolean = true,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    SettingsScreen(
        theme = state.themeMode,
        fontScale = state.fontScale,
        highContrast = state.highContrast,
        reduceMotion = state.reduceMotion,
        ocrRoute = state.ocrRoute,
        analytics = state.analyticsEnabled,
        crash = state.crashReportingEnabled,
        onTheme = viewModel::setTheme,
        onFont = viewModel::setFont,
        onContrast = viewModel::setContrast,
        onMotion = viewModel::setMotion,
        onOcr = viewModel::setOcr,
        onAnalytics = viewModel::setAnalytics,
        onCrash = viewModel::setCrash,
        onPrivacy = onPrivacy,
        onVoiceTest = onVoiceTest,
        onBack = onBack,
        updateState = updateState,
        updateManager = viewModel.updates,
        onCheckUpdate = viewModel::checkUpdate,
        onDownloadUpdate = viewModel::downloadUpdate,
        showBack = showBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    theme: ThemeMode,
    fontScale: Float,
    highContrast: Boolean,
    reduceMotion: Boolean,
    ocrRoute: OcrRoute,
    analytics: Boolean,
    crash: Boolean,
    onTheme: (ThemeMode) -> Unit,
    onFont: (Float) -> Unit,
    onContrast: (Boolean) -> Unit,
    onMotion: (Boolean) -> Unit,
    onOcr: (OcrRoute) -> Unit,
    onAnalytics: (Boolean) -> Unit,
    onCrash: (Boolean) -> Unit,
    onPrivacy: () -> Unit,
    onVoiceTest: () -> Unit,
    onBack: () -> Unit,
    updateState: UpdateUiState = UpdateUiState(),
    updateManager: AppUpdateManager? = null,
    onCheckUpdate: () -> Unit = {},
    onDownloadUpdate: () -> Unit = {},
    showBack: Boolean = true,
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    if (showBack) {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ScreenHeader("Settings", "Adjust text size, contrast, and how pages are read.")
            Text("Theme", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(selected = theme == mode, onClick = { onTheme(mode) }, label = { Text(mode.name.lowercase().replaceFirstChar { it.titlecase() }) })
                }
            }
            Text("Text size", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1.0f, 1.25f, 1.5f, 1.75f).forEach { scale ->
                    FilterChip(selected = fontScale == scale, onClick = { onFont(scale) }, label = { Text("${(scale * 100).toInt()}%") })
                }
            }
            SettingRow("High contrast") {
                Switch(checked = highContrast, onCheckedChange = onContrast)
            }
            SettingRow("Reduce motion") {
                Switch(checked = reduceMotion, onCheckedChange = onMotion)
            }
            Text("Page reading", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OcrRoute.entries.forEach { route ->
                    FilterChip(selected = ocrRoute == route, onClick = { onOcr(route) }, label = { Text(route.name.lowercase().replaceFirstChar { it.titlecase() }) })
                }
            }
            updateManager?.let { manager ->
                UpdateSection(
                    state = updateState,
                    manager = manager,
                    onCheck = onCheckUpdate,
                    onDownload = onDownloadUpdate,
                )
            }
            LargeButton("Compare voices", onVoiceTest, tonal = true)
            LargeButton("Privacy", onPrivacy, tonal = true)
            SettingRow("Anonymous diagnostics") {
                Switch(checked = analytics, onCheckedChange = onAnalytics)
            }
            SettingRow("Crash reports") {
                Switch(checked = crash, onCheckedChange = onCrash)
            }
        }
    }
}

@Composable
fun PrivacyRoute(onBack: () -> Unit) {
    PrivacyScreen(onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ScreenHeader("Your books stay on this phone")
            Text("Books, notes, bookmarks, and reading position are stored locally.")
            Text("Voice recordings are uploaded only when you create a voice profile, and only through the app’s own server. Provider API keys never live in the app.")
            Text("You can delete a book or a voice profile at any time. Deleting a voice also deletes cached speech made with that voice.")
            Text("The app does not log book contents, voice recordings, passwords, or access tokens.")
            Text("Cloud reading and custom-voice creation need an internet connection. Already saved books and cached audio work offline.")
        }
    }
}
