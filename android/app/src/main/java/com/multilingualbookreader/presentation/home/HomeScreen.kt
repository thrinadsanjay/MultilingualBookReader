package com.multilingualbookreader.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.multilingualbookreader.R
import com.multilingualbookreader.presentation.components.ContinueListeningCard
import com.multilingualbookreader.presentation.components.HeroActionCard
import com.multilingualbookreader.presentation.components.OpenBookDecoration
import com.multilingualbookreader.presentation.components.SectionLinkRow
import com.multilingualbookreader.presentation.components.StatusBadge
import com.multilingualbookreader.presentation.components.VoiceHomeCard
import com.multilingualbookreader.presentation.theme.LocalBrand
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
    val brand = LocalBrand.current
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Box(Modifier.fillMaxWidth()) {
                OpenBookDecoration(Modifier.align(Alignment.TopEnd))
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp, end = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Book Reader", style = MaterialTheme.typography.displaySmall)
                        Text(
                            "Open a book and listen.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = brand.muted,
                        )
                    }
                    StatusBadge(state.online)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                HeroActionCard(
                    title = stringResource(R.string.scan_book),
                    subtitle = "Use camera to scan book pages",
                    icon = Icons.Outlined.DocumentScanner,
                    onClick = onScan,
                    modifier = Modifier.weight(1f),
                    background = brand.scanGradient,
                    iconTint = androidx.compose.ui.graphics.Color.White,
                    iconWell = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.22f),
                    subtitleColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.82f),
                    arrowWell = androidx.compose.ui.graphics.Color(0x33000000),
                    arrowTint = androidx.compose.ui.graphics.Color.White,
                )
                HeroActionCard(
                    title = stringResource(R.string.import_pdf),
                    subtitle = "Import books from your files",
                    icon = Icons.Outlined.PictureAsPdf,
                    onClick = onImportPdf,
                    modifier = Modifier.weight(1f),
                    backgroundColor = brand.importBackground,
                    iconTint = brand.teal,
                    iconWell = brand.teal.copy(alpha = 0.16f),
                    subtitleColor = brand.muted,
                    arrowWell = brand.tealDeep,
                    arrowTint = brand.teal,
                    titleColor = MaterialTheme.colorScheme.onBackground,
                )
            }

            SectionLinkRow(
                title = stringResource(R.string.my_voice),
                action = "Manage Voice  >",
                onAction = onVoice,
                actionColor = brand.orange,
            )
            VoiceHomeCard(
                name = state.voice?.name ?: "No voice yet",
                detail = state.voice?.status?.name?.lowercase()?.replaceFirstChar { it.titlecase() }
                    ?: "Create your own voice to hear books in your speech.",
                onCreate = onVoice,
            )

            SectionLinkRow(
                title = "Continue Listening",
                action = "View all  >",
                onAction = onLibrary,
                actionColor = brand.teal,
            )
            state.continueReading?.let { item ->
                val page = (item.progressPercent * item.book.totalPages / 100).coerceAtLeast(1)
                ContinueListeningCard(
                    title = item.book.title,
                    pageLabel = "Page $page of ${item.book.totalPages}  •  ${item.progressPercent}%",
                    progressPercent = item.progressPercent,
                    onPlay = { onContinue(item.book.id) },
                )
            } ?: Text(
                "Imported and scanned books will show up here.",
                style = MaterialTheme.typography.bodySmall,
                color = brand.muted,
            )

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
