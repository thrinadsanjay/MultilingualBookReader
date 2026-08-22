@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.multilingualbookreader.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.MotionPhotosOff
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.multilingualbookreader.BuildConfig
import com.multilingualbookreader.domain.model.OcrRoute
import com.multilingualbookreader.domain.model.ThemeMode
import com.multilingualbookreader.presentation.components.ScreenHeader
import com.multilingualbookreader.presentation.components.SecondaryButton
import com.multilingualbookreader.presentation.components.SettingsRow
import com.multilingualbookreader.presentation.components.SettingsSection
import com.multilingualbookreader.presentation.components.ToggleRow
import com.multilingualbookreader.presentation.theme.LocalBrand
import com.multilingualbookreader.presentation.theme.LocalDimens
import com.multilingualbookreader.presentation.update.UpdateSection
import com.multilingualbookreader.update.AppUpdateManager
import com.multilingualbookreader.update.UpdateUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsRoute(
    onPrivacy: () -> Unit,
    onVoiceTest: () -> Unit,
    onUpdates: () -> Unit,
    onHelp: () -> Unit,
    onAbout: () -> Unit,
    onBack: () -> Unit,
    showBack: Boolean = true,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SettingsScreen(
        theme = state.themeMode,
        fontScale = state.fontScale,
        highContrast = state.highContrast,
        reduceMotion = state.reduceMotion,
        ocrRoute = state.ocrRoute,
        languageTag = state.defaultLanguageTag,
        analytics = state.analyticsEnabled,
        crash = state.crashReportingEnabled,
        onTheme = viewModel::setTheme,
        onFont = viewModel::setFont,
        onContrast = viewModel::setContrast,
        onMotion = viewModel::setMotion,
        onOcr = viewModel::setOcr,
        onLanguage = viewModel::setLanguage,
        onAnalytics = viewModel::setAnalytics,
        onCrash = viewModel::setCrash,
        onPrivacy = onPrivacy,
        onVoiceTest = onVoiceTest,
        onUpdates = onUpdates,
        onHelp = onHelp,
        onAbout = onAbout,
        onDeleteAll = viewModel::deleteAllData,
        onBack = onBack,
        showBack = showBack,
    )
}

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
    languageTag: String = "AUTO",
    onLanguage: (String) -> Unit = {},
    onUpdates: () -> Unit = {},
    onHelp: () -> Unit = {},
    onAbout: () -> Unit = {},
    onDeleteAll: () -> Unit = {},
    showBack: Boolean = true,
) {
    val brand = LocalBrand.current
    val dimens = LocalDimens.current
    var picker by remember { mutableStateOf<SettingsPicker?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = brand.background,
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = dimens.screen, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text("Settings", style = MaterialTheme.typography.displaySmall, color = brand.textPrimary)
            SettingsSection("Reading preferences") {
                SettingsRow("Page reading", Icons.Outlined.DocumentScanner, ocrRoute.label()) { picker = SettingsPicker.Ocr }
                SettingsRow("Default language", Icons.Outlined.Language, languageTag.labelLanguage()) { picker = SettingsPicker.Language }
                SettingsRow("Text size", Icons.Outlined.TextFields, "${(fontScale * 100).toInt()}%") { picker = SettingsPicker.Font }
                SettingsRow("Theme", Icons.Outlined.Palette, theme.label()) { picker = SettingsPicker.Theme }
                ToggleRow("High contrast", Icons.Outlined.Contrast, highContrast, onContrast)
                ToggleRow("Reduce motion", Icons.Outlined.MotionPhotosOff, reduceMotion, onMotion)
            }
            SettingsSection("App") {
                SettingsRow("Check for updates", Icons.Outlined.SystemUpdate, "v${BuildConfig.VERSION_NAME}") { onUpdates() }
                SettingsRow("Privacy", Icons.Outlined.PrivacyTip, onClick = onPrivacy)
                SettingsRow("Help & feedback", Icons.AutoMirrored.Outlined.HelpOutline, onClick = onHelp)
                SettingsRow("About", Icons.Outlined.Info, onClick = onAbout)
            }
            SettingsRow(
                label = "Delete all data",
                icon = Icons.Outlined.Delete,
                onClick = { confirmDelete = true },
            )
        }
    }
    when (picker) {
        SettingsPicker.Theme -> ChoiceDialog("Theme", ThemeMode.entries.map { it.name to it.label() }, theme.name, { onTheme(ThemeMode.valueOf(it)); picker = null }, { picker = null })
        SettingsPicker.Font -> ChoiceDialog("Text size", listOf(1.0f, 1.25f, 1.5f, 1.75f).map { it.toString() to "${(it * 100).toInt()}%" }, fontScale.toString(), { onFont(it.toFloat()); picker = null }, { picker = null })
        SettingsPicker.Ocr -> ChoiceDialog("Page reading", OcrRoute.entries.map { it.name to it.label() }, ocrRoute.name, { onOcr(OcrRoute.valueOf(it)); picker = null }, { picker = null })
        SettingsPicker.Language -> ChoiceDialog(
            "Default language",
            listOf("AUTO" to "Auto detect", "en" to "English", "hi" to "Hindi", "te" to "Telugu"),
            languageTag,
            { onLanguage(it); picker = null },
            { picker = null },
        )
        null -> Unit
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete all data?") },
            text = { Text("This removes books, page text, cached speech, voice profiles, reading progress, bookmarks, and settings. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDeleteAll() }) { Text("Delete everything") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

private enum class SettingsPicker { Theme, Font, Ocr, Language }

private fun ThemeMode.label() = name.lowercase().replaceFirstChar { it.titlecase() }
private fun OcrRoute.label() = when (this) {
    OcrRoute.AUTO -> "Auto"
    OcrRoute.ON_DEVICE -> "On device"
    OcrRoute.CLOUD -> "Cloud"
}
private fun String.labelLanguage() = when (this) {
    "en" -> "English"
    "hi" -> "Hindi"
    "te" -> "Telugu"
    else -> "Auto detect"
}

@Composable
private fun ChoiceDialog(
    title: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { (value, label) ->
                    TextButton(onClick = { onSelect(value) }) {
                        Text(if (value == selected) "●  $label" else "○  $label")
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
fun UpdatesRoute(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    UpdatesScreen(
        updateState = updateState,
        updateManager = viewModel.updates,
        onCheck = viewModel::checkUpdate,
        onDownload = viewModel::downloadUpdate,
        onBack = onBack,
    )
}

@Composable
fun UpdatesScreen(
    updateState: UpdateUiState,
    updateManager: AppUpdateManager,
    onCheck: () -> Unit,
    onDownload: () -> Unit,
    onBack: () -> Unit,
) {
    val brand = LocalBrand.current
    Scaffold(
        containerColor = brand.background,
        topBar = {
            TopAppBar(
                title = { Text("Updates") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            UpdateSection(state = updateState, manager = updateManager, onCheck = onCheck, onDownload = onDownload)
        }
    }
}

@Composable
fun PrivacyRoute(
    analytics: Boolean = false,
    crash: Boolean = false,
    onAnalytics: (Boolean) -> Unit = {},
    onCrash: (Boolean) -> Unit = {},
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PrivacyScreen(
        analytics = state.analyticsEnabled,
        crash = state.crashReportingEnabled,
        onAnalytics = viewModel::setAnalytics,
        onCrash = viewModel::setCrash,
        onBack = onBack,
    )
}

@Composable
fun PrivacyScreen(
    onBack: () -> Unit,
    analytics: Boolean = false,
    crash: Boolean = false,
    onAnalytics: (Boolean) -> Unit = {},
    onCrash: (Boolean) -> Unit = {},
) {
    val brand = LocalBrand.current
    Scaffold(
        containerColor = brand.background,
        topBar = {
            TopAppBar(
                title = { Text("Privacy") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ScreenHeader("Your books stay on this phone")
            Text("Books, notes, bookmarks, and reading position are stored locally.", color = brand.textSecondary)
            Text("Voice recordings are uploaded only when you create a voice profile, and only through the app’s own server.", color = brand.textSecondary)
            SettingsSection("Privacy controls") {
                ToggleRow("Anonymous diagnostics", Icons.Outlined.Info, analytics, onAnalytics)
                ToggleRow("Crash reports", Icons.Outlined.Info, crash, onCrash)
            }
        }
    }
}

@Composable
fun HelpRoute(onBack: () -> Unit) {
    SimpleInfoScreen("Help & feedback", "For issues, open a GitHub issue on MultilingualBookReader or email the maintainer. Include the app version, not book contents or voice recordings.", onBack)
}

@Composable
fun AboutRoute(onBack: () -> Unit) {
    SimpleInfoScreen("About", "Book Reader ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}). GPL-3.0. English, Hindi, and Telugu reading with optional custom voice.", onBack)
}

@Composable
private fun SimpleInfoScreen(title: String, body: String, onBack: () -> Unit) {
    val brand = LocalBrand.current
    Scaffold(
        containerColor = brand.background,
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(body, color = brand.textSecondary, style = MaterialTheme.typography.bodyLarge)
            SecondaryButton("Back", onBack)
        }
    }
}
