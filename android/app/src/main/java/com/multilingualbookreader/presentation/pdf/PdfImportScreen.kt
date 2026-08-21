package com.multilingualbookreader.presentation.pdf

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.multilingualbookreader.presentation.components.LargeButton
import com.multilingualbookreader.presentation.components.ScreenHeader

@Composable
fun PdfImportRoute(
    onOpenReader: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: PdfImportViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.bookId) {
        state.bookId?.let(onOpenReader)
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let(viewModel::import)
    }
    PdfImportScreen(
        state = state,
        onPick = { picker.launch(arrayOf("application/pdf")) },
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfImportScreen(
    state: PdfImportUiState,
    onPick: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import PDF") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ScreenHeader("Import a book", "Choose a PDF. Text pages are read directly. Scanned pages are converted automatically.")
            if (state.busy) {
                CircularProgressIndicator()
                Text(state.message ?: "Processing book...")
                if (state.totalPages > 0) {
                    Text("Page ${state.currentPage} / ${state.totalPages}")
                }
            } else {
                LargeButton("Choose PDF", onPick)
                state.error?.let { Text(it) }
            }
        }
    }
}
