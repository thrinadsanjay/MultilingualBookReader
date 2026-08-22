package com.multilingualbookreader.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.multilingualbookreader.R
import com.multilingualbookreader.presentation.components.ActionCard
import com.multilingualbookreader.presentation.components.OnlineBanner
import com.multilingualbookreader.presentation.update.UpdateSection
import com.multilingualbookreader.update.AppUpdateManager
import com.multilingualbookreader.update.UpdateUiState

@Composable
fun HomeRoute(
    onScan: () -> Unit,
    onImportPdf: () -> Unit,
    onLibrary: () -> Unit,
    onVoice: () -> Unit,
    onSettings: () -> Unit,
    onContinue: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    HomeScreen(
        state = state,
        updateState = updateState,
        updateManager = viewModel.updates,
        onScan = onScan,
        onImportPdf = onImportPdf,
        onLibrary = onLibrary,
        onVoice = onVoice,
        onSettings = onSettings,
        onContinue = onContinue,
        onCheckUpdate = viewModel::checkUpdate,
        onDownloadUpdate = viewModel::downloadUpdate,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onScan: () -> Unit,
    onImportPdf: () -> Unit,
    onLibrary: () -> Unit,
    onVoice: () -> Unit,
    onSettings: () -> Unit,
    onContinue: (String) -> Unit,
    updateState: UpdateUiState = UpdateUiState(),
    updateManager: AppUpdateManager? = null,
    onCheckUpdate: () -> Unit = {},
    onDownloadUpdate: () -> Unit = {},
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Book Reader", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Open a book and listen",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    Row(Modifier.padding(end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        OnlineBanner(state.online)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                ActionCard(
                    title = stringResource(R.string.scan_book),
                    subtitle = "Camera",
                    icon = Icons.Outlined.DocumentScanner,
                    onClick = onScan,
                    highlighted = true,
                    modifier = Modifier.weight(1f),
                )
                ActionCard(
                    title = stringResource(R.string.import_pdf),
                    subtitle = "From files",
                    icon = Icons.Outlined.PictureAsPdf,
                    onClick = onImportPdf,
                    modifier = Modifier.weight(1f),
                )
            }

            state.continueReading?.let { item ->
                Text(stringResource(R.string.continue_reading), style = MaterialTheme.typography.titleMedium)
                Card(
                    onClick = { onContinue(item.book.id) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            Icons.Outlined.PlayCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Column(Modifier.weight(1f)) {
                            Text(item.book.title, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Page ${(item.progressPercent * item.book.totalPages / 100).coerceAtLeast(1)} of ${item.book.totalPages}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            Text(stringResource(R.string.my_voice), style = MaterialTheme.typography.titleMedium)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        Icons.Outlined.RecordVoiceOver,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                    Column(Modifier.weight(1f)) {
                        Text(state.voice?.name ?: "No voice yet", style = MaterialTheme.typography.titleMedium)
                        Text(
                            state.voice?.status?.name?.lowercase()?.replaceFirstChar { it.titlecase() }
                                ?: "Create a voice to hear books in your speech.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = onVoice) {
                        Text(stringResource(R.string.manage_voice))
                    }
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
        }
    }
}
