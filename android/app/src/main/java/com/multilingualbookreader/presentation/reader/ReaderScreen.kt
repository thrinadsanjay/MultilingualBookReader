package com.multilingualbookreader.presentation.reader

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.multilingualbookreader.camera.PageImageProcessor
import com.multilingualbookreader.domain.model.BookPage
import com.multilingualbookreader.domain.model.ProcessingStatus
import com.multilingualbookreader.presentation.components.EmptyState
import com.multilingualbookreader.presentation.theme.LocalBrand

@Composable
fun ReaderRoute(
    bookId: String,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onAddPages: () -> Unit = {},
    viewModel: ReaderViewModel = hiltViewModel(),
) {
    val state by viewModel.ui.collectAsStateWithLifecycle()
    LaunchedEffect(bookId) { viewModel.load(bookId) }
    ReaderScreen(
        state = state,
        onBack = onBack,
        onSearch = onSearch,
        onAddPages = onAddPages,
        onPlay = viewModel::play,
        onPause = viewModel::pause,
        onResume = viewModel::resume,
        onPrevSentence = viewModel::previousSentence,
        onNextSentence = viewModel::nextSentence,
        onSkipParagraph = viewModel::skipParagraph,
        onRepeat = viewModel::repeatSentence,
        onRestart = viewModel::restartPage,
        onSpeed = viewModel::setSpeed,
        onPrevPage = viewModel::previousPage,
        onNextPage = viewModel::nextPage,
        onGoToPage = viewModel::goToPage,
        onBookmark = viewModel::bookmark,
        onNoteDraft = viewModel::updateNoteDraft,
        onSaveNote = viewModel::saveNote,
        onRename = viewModel::rename,
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
    onPrevSentence: () -> Unit,
    onNextSentence: () -> Unit,
    onSkipParagraph: () -> Unit,
    onRepeat: () -> Unit,
    onRestart: () -> Unit,
    onSpeed: (Float) -> Unit,
    onPrevPage: () -> Unit,
    onNextPage: () -> Unit,
    onBookmark: () -> Unit,
    onNoteDraft: (String) -> Unit,
    onSaveNote: () -> Unit,
    onAddPages: () -> Unit = {},
    onGoToPage: (Int) -> Unit = {},
    onRename: (String) -> Unit = {},
) {
    val brand = LocalBrand.current
    val page = state.pages.getOrNull(state.pageIndex)
    var menuOpen by remember { mutableStateOf(false) }
    var noteOpen by remember { mutableStateOf(false) }
    var renameOpen by remember { mutableStateOf(false) }
    var preferImage by remember { mutableStateOf(true) }
    val pageCount = state.pages.size
    val pagerState = rememberPagerState(
        initialPage = state.pageIndex.coerceAtLeast(0),
        pageCount = { pageCount.coerceAtLeast(1) },
    )
    LaunchedEffect(state.pageIndex, pageCount) {
        if (pageCount == 0) return@LaunchedEffect
        val target = state.pageIndex.coerceIn(0, pageCount - 1)
        if (pagerState.currentPage != target) pagerState.scrollToPage(target)
    }
    LaunchedEffect(pagerState.settledPage, pageCount) {
        if (pageCount == 0) return@LaunchedEffect
        if (pagerState.settledPage != state.pageIndex) onGoToPage(pagerState.settledPage)
    }
    Scaffold(
        containerColor = brand.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.book?.title ?: "Reader",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onAddPages) {
                        Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = "Add pages")
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }
                        ReaderOverflowMenu(
                            expanded = menuOpen,
                            onDismiss = { menuOpen = false },
                            hasImage = !page?.imagePath.isNullOrBlank(),
                            preferImage = preferImage,
                            onAddPages = { menuOpen = false; onAddPages() },
                            onRename = { menuOpen = false; renameOpen = true },
                            onAddNote = { menuOpen = false; noteOpen = true },
                            onBookmark = { menuOpen = false; onBookmark() },
                            onSearch = { menuOpen = false; onSearch() },
                            onToggleView = {
                                menuOpen = false
                                preferImage = !preferImage
                            },
                            onPrevSentence = { menuOpen = false; onPrevSentence() },
                            onNextSentence = { menuOpen = false; onNextSentence() },
                            onRepeat = { menuOpen = false; onRepeat() },
                            onSkipParagraph = { menuOpen = false; onSkipParagraph() },
                            onRestart = { menuOpen = false; onRestart() },
                            speed = state.playback.speed,
                            onSpeed = { onSpeed(it); menuOpen = false },
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = brand.background),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(brand.surfaceSecondary),
            ) {
                if (state.pages.isEmpty()) {
                    EmptyState(
                        title = "No pages yet",
                        message = "Scan a page or import a PDF, then it will open here like a book.",
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        primary = "Add pages",
                        onPrimary = onAddPages,
                        modifier = Modifier.align(Alignment.Center),
                    )
                } else {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 36.dp, vertical = 12.dp),
                        pageSpacing = 16.dp,
                    ) { index ->
                        val item = state.pages.getOrNull(index)
                        BookPageSpread(
                            state = state,
                            page = item,
                            preferImage = preferImage,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    PageTurnButton(
                        icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        label = "Previous page",
                        enabled = state.pageIndex > 0,
                        onClick = onPrevPage,
                        modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp),
                    )
                    val last = state.pageIndex >= state.pages.lastIndex
                    PageTurnButton(
                        icon = if (last) Icons.Default.Add else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        label = if (last) "Add pages" else "Next page",
                        enabled = true,
                        onClick = if (last) onAddPages else onNextPage,
                        modifier = Modifier.align(Alignment.CenterEnd).padding(end = 4.dp),
                    )
                }
            }
            state.error?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                )
            }
            ReaderListenBar(
                state = state,
                onPlay = onPlay,
                onPause = onPause,
                onResume = onResume,
            )
        }
    }
    if (renameOpen) {
        com.multilingualbookreader.presentation.library.NameBookDialog(
            title = "Rename book",
            initial = state.book?.title.orEmpty(),
            confirm = "Save",
            onConfirm = {
                onRename(it)
                renameOpen = false
            },
            onDismiss = { renameOpen = false },
        )
    }
    if (noteOpen) {
        NoteDialog(
            state = state,
            onDraft = onNoteDraft,
            onSave = {
                onSaveNote()
                noteOpen = false
            },
            onDismiss = { noteOpen = false },
        )
    }
}

@Composable
private fun ReaderOverflowMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    hasImage: Boolean,
    preferImage: Boolean,
    onAddPages: () -> Unit,
    onRename: () -> Unit,
    onAddNote: () -> Unit,
    onBookmark: () -> Unit,
    onSearch: () -> Unit,
    onToggleView: () -> Unit,
    onPrevSentence: () -> Unit,
    onNextSentence: () -> Unit,
    onRepeat: () -> Unit,
    onSkipParagraph: () -> Unit,
    onRestart: () -> Unit,
    speed: Float,
    onSpeed: (Float) -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text("Add pages") },
            onClick = onAddPages,
            leadingIcon = { Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null) },
        )
        DropdownMenuItem(
            text = { Text("Rename book") },
            onClick = onRename,
        )
        DropdownMenuItem(
            text = { Text("Add a note") },
            onClick = onAddNote,
            leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Notes, contentDescription = null) },
        )
        DropdownMenuItem(
            text = { Text("Bookmark this page") },
            onClick = onBookmark,
            leadingIcon = { Icon(Icons.Outlined.Bookmark, contentDescription = null) },
        )
        DropdownMenuItem(
            text = { Text("Search") },
            onClick = onSearch,
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        )
        if (hasImage) {
            DropdownMenuItem(
                text = { Text(if (preferImage) "Show text" else "Show original page") },
                onClick = onToggleView,
                leadingIcon = {
                    Icon(
                        if (preferImage) Icons.Outlined.TextFields else Icons.Outlined.Image,
                        contentDescription = null,
                    )
                },
            )
        }
        HorizontalDivider()
        DropdownMenuItem(text = { Text("Previous sentence") }, onClick = onPrevSentence)
        DropdownMenuItem(text = { Text("Next sentence") }, onClick = onNextSentence)
        DropdownMenuItem(text = { Text("Repeat") }, onClick = onRepeat)
        DropdownMenuItem(text = { Text("Skip paragraph") }, onClick = onSkipParagraph)
        DropdownMenuItem(text = { Text("Restart page") }, onClick = onRestart)
        HorizontalDivider()
        Text(
            "Listening speed",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        listOf(0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f).forEach { value ->
            DropdownMenuItem(
                text = { Text("${value}x", fontWeight = if (speed == value) FontWeight.SemiBold else FontWeight.Normal) },
                onClick = { onSpeed(value) },
            )
        }
    }
}

@Composable
private fun BookPageSpread(
    state: ReaderUiState,
    page: BookPage?,
    preferImage: Boolean,
    modifier: Modifier = Modifier,
) {
    val paper = Color(0xFFF6EFE3)
    val ink = Color(0xFF2A2218)
    val pageImage = remember(page?.imagePath) {
        page?.imagePath?.let { PageImageProcessor.decodePageFile(it) }
    }
    Box(
        modifier
            .shadow(10.dp, RoundedCornerShape(4.dp), clip = false)
            .clip(RoundedCornerShape(4.dp))
            .background(paper)
            .semantics { contentDescription = pageLabel(state, page) },
        contentAlignment = Alignment.Center,
    ) {
        if (preferImage && pageImage != null) {
            Image(
                bitmap = pageImage.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().background(paper),
            )
        } else {
            BookTextPage(state = state, page = page, ink = ink)
        }
    }
}

@Composable
private fun BookTextPage(
    state: ReaderUiState,
    page: BookPage?,
    ink: Color,
) {
    val current = state.playback.segments.getOrNull(state.playback.currentSegmentIndex)
    val text = page?.text.orEmpty()
    val annotated = buildAnnotatedString {
        if (current == null || text.isEmpty()) {
            append(text.ifBlank { emptyPageMessage(state, page) })
        } else {
            val start = current.startOffset.coerceIn(0, text.length)
            val end = current.endOffset.coerceIn(start, text.length)
            append(text.substring(0, start))
            withStyle(SpanStyle(background = Color(0xFFE8C9A4), fontWeight = FontWeight.SemiBold)) {
                append(text.substring(start, end))
            }
            append(text.substring(end))
        }
    }
    Text(
        text = annotated,
        color = ink,
        fontFamily = FontFamily.Serif,
        fontSize = 18.sp,
        lineHeight = 32.sp,
        textAlign = TextAlign.Start,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 24.dp),
    )
}

@Composable
private fun PageTurnButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color(0xCC1A1A1F))
            .semantics { contentDescription = label },
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (enabled) Color(0xFFF3F1ED) else Color(0x66F3F1ED),
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun ReaderListenBar(
    state: ReaderUiState,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
) {
    val brand = LocalBrand.current
    val playing = state.playback.isPlaying
    val label = when {
        state.pages.isEmpty() -> "No pages yet"
        else -> "${state.pageIndex + 1}  /  ${state.pages.size} · ${state.selectedVoice.name}"
    }
    Row(
        Modifier
            .fillMaxWidth()
            .background(brand.background)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = brand.textSecondary, modifier = Modifier.weight(1f))
        IconButton(
            onClick = {
                when {
                    playing -> onPause()
                    state.playback.segments.isEmpty() -> onPlay()
                    else -> onResume()
                }
            },
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(brand.accent),
        ) {
            Icon(
                if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = when {
                    playing -> "Pause"
                    state.loadingSpeech -> "Preparing…"
                    else -> "Play"
                },
                tint = brand.onAccent,
            )
        }
        Spacer(Modifier.width(52.dp))
    }
}

@Composable
private fun NoteDialog(
    state: ReaderUiState,
    onDraft: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add a note") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (state.notes.isNotEmpty()) {
                    state.notes.takeLast(4).forEach { note ->
                        Text("Page ${note.pageNumber}: ${note.body}", style = MaterialTheme.typography.bodySmall)
                    }
                    HorizontalDivider()
                }
                OutlinedTextField(
                    value = state.noteDraft,
                    onValueChange = onDraft,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Note") },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onSave) { Text("Save note") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

private fun pageLabel(state: ReaderUiState, page: BookPage?): String =
    if (state.pages.isEmpty()) "No pages yet" else "Page ${page?.pageNumber ?: state.pageIndex + 1} / ${state.pages.size}"

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
