package com.multilingualbookreader.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.multilingualbookreader.R
import com.multilingualbookreader.presentation.components.LargeButton
import com.multilingualbookreader.presentation.components.OnlineBanner
import com.multilingualbookreader.presentation.components.ScreenHeader

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
    HomeScreen(
        state = state,
        onScan = onScan,
        onImportPdf = onImportPdf,
        onLibrary = onLibrary,
        onVoice = onVoice,
        onSettings = onSettings,
        onContinue = onContinue,
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
) {
    Scaffold { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            OnlineBanner(state.online)
            Column(
                Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ScreenHeader("Book Reader", "Open a book, press play, and listen.")
                LargeButton(stringResource(R.string.scan_book), onScan)
                LargeButton(stringResource(R.string.import_pdf), onImportPdf, tonal = true)
                LargeButton(stringResource(R.string.my_library), onLibrary, tonal = true)

                state.continueReading?.let { item ->
                    Text(stringResource(R.string.continue_reading), style = MaterialTheme.typography.headlineMedium)
                    Card(onClick = { onContinue(item.book.id) }) {
                        Column(Modifier.padding(20.dp)) {
                            Text(item.book.title, style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Page ${item.book.let { /* progress */ }}".let {
                                    "Page ${(item.progressPercent * item.book.totalPages / 100).coerceAtLeast(1)} / ${item.book.totalPages}"
                                },
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Spacer(Modifier.height(12.dp))
                            LargeButton(stringResource(R.string.continue_action), { onContinue(item.book.id) })
                        }
                    }
                }

                Text(stringResource(R.string.my_voice), style = MaterialTheme.typography.headlineMedium)
                Card {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(state.voice?.name ?: "No voice yet", style = MaterialTheme.typography.titleLarge)
                        Text(state.voice?.status?.name?.lowercase()?.replaceFirstChar { it.titlecase() } ?: "Create a voice to hear books in your own speech.")
                        LargeButton(stringResource(R.string.manage_voice), onVoice, tonal = true)
                    }
                }
                LargeButton(stringResource(R.string.settings), onSettings, tonal = true)
            }
        }
    }
}
