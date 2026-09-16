package com.multilingualbookreader.presentation.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.multilingualbookreader.domain.model.BookPage
import com.multilingualbookreader.domain.model.ProcessingStatus
import com.multilingualbookreader.presentation.components.LargeButton

@Composable
fun ReaderRoute(
    bookId: String,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    viewModel: ReaderViewModel = hiltViewModel(),
) {
    val state by viewModel.ui.collectAsStateWithLifecycle()
    LaunchedEffect(bookId) { viewModel.load(bookId) }
    ReaderScreen(
        state = state,
        onBack = onBack,
        onSearch = onSearch,
        onPlay = viewModel::play,
        onPause = viewModel::pause,
        onResume = viewModel::resume,
        onPrev = viewModel::previousSentence,
        onNext = viewModel::nextSentence,
        onSkipParagraph = viewModel::skipParagraph,
        onRepeat = viewModel::repeatSentence,
        onRestart = viewModel::restartPage,
        onSpeed = viewModel::setSpeed,
        onPrevPage = viewModel::previousPage,
        onNextPage = viewModel::nextPage,
        onBookmark = viewModel::bookmark,
        onNoteDraft = viewModel::updateNoteDraft,
        onSaveNote = viewModel::saveNote,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    state: ReaderUiState,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onSkipParagraph: () -> Unit,
    onRepeat: () -> Unit,
    onRestart: () -> Unit,
    onSpeed: (Float) -> Unit,
    onPrevPage: () -> Unit,
    onNextPage: () -> Unit,
    onBookmark: () -> Unit,
    onNoteDraft: (String) -> Unit,
    onSaveNote: () -> Unit,
) {
    val page = state.pages.getOrNull(state.pageIndex)
    val current = state.playback.segments.getOrNull(state.playback.currentSegmentIndex)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.book?.title ?: "Reader") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(onClick = onSearch) { Icon(Icons.Default.Search, contentDescription = "Search") }
                    IconButton(onClick = onBookmark) { Icon(Icons.Default.Bookmark, contentDescription = "Bookmark page") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                if (state.pages.isEmpty()) "No pages yet" else "Page ${state.pageIndex + 1} / ${state.pages.size}",
                style = MaterialTheme.typography.titleLarge,
            )
            val text = page?.text.orEmpty()
            val annotated = buildAnnotatedString {
                if (current == null || text.isEmpty()) {
                    append(text.ifBlank { emptyPageMessage(state, page) })
                } else {
                    val start = current.startOffset.coerceIn(0, text.length)
                    val end = current.endOffset.coerceIn(start, text.length)
                    append(text.substring(0, start))
                    withStyle(SpanStyle(background = MaterialTheme.colorScheme.primaryContainer, fontWeight = FontWeight.SemiBold)) {
                        append(text.substring(start, end))
                    }
                    append(text.substring(end))
                }
            }
            Text(
                text = annotated,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            )
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                LargeButton("Previous", onPrev, modifier = Modifier.weight(1f), tonal = true)
                if (state.playback.isPlaying) {
                    LargeButton("Pause", onPause, modifier = Modifier.weight(1f))
                } else {
                    LargeButton(if (state.loadingSpeech) "Preparing…" else "Play", if (state.playback.segments.isEmpty()) onPlay else onResume, modifier = Modifier.weight(1f))
                }
                LargeButton("Next", onNext, modifier = Modifier.weight(1f), tonal = true)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f).forEach { speed ->
                    FilterChip(
                        selected = state.playback.speed == speed,
                        onClick = { onSpeed(speed) },
                        label = { Text("${speed}x") },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                LargeButton("Repeat", onRepeat, modifier = Modifier.weight(1f), tonal = true)
                LargeButton("Skip paragraph", onSkipParagraph, modifier = Modifier.weight(1f), tonal = true)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                LargeButton("Previous page", onPrevPage, modifier = Modifier.weight(1f), tonal = true)
                LargeButton("Restart page", onRestart, modifier = Modifier.weight(1f), tonal = true)
                LargeButton("Next page", onNextPage, modifier = Modifier.weight(1f), tonal = true)
            }
            OutlinedTextField(
                value = state.noteDraft,
                onValueChange = onNoteDraft,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Add a note") },
            )
            LargeButton("Save note", onSaveNote, tonal = true)
            if (state.bookmarks.isNotEmpty()) {
                Text("Bookmarks: " + state.bookmarks.joinToString { "Page ${it.pageNumber}" })
            }
        }
    }
}

/**
 * Explains why a page is blank. "No text yet" used to appear whether the import produced nothing,
 * recognition failed, or the page really was empty, which gave no clue what to do next.
 */
private fun emptyPageMessage(state: ReaderUiState, page: BookPage?): String = when {
    state.book == null -> "Opening this book…"
    page == null -> "This book has no pages yet. Scan a page or import a PDF again."
    page.processingStatus == ProcessingStatus.PROCESSING -> "Still reading this page…"
    page.processingStatus == ProcessingStatus.FAILED ->
        page.errorMessage ?: "This page could not be read. Try scanning it again in better light."
    else -> "This page has no text on it."
}
