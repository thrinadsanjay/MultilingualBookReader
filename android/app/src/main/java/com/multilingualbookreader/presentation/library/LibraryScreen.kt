package com.multilingualbookreader.presentation.library

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.multilingualbookreader.domain.model.BookSource
import com.multilingualbookreader.domain.model.LibraryBook
import com.multilingualbookreader.presentation.components.BookReaderCard
import com.multilingualbookreader.presentation.components.EmptyState
import com.multilingualbookreader.presentation.components.FilterChipItem
import com.multilingualbookreader.presentation.components.ReaderProgressBar
import com.multilingualbookreader.presentation.theme.LocalBrand
import com.multilingualbookreader.presentation.theme.LocalDimens

enum class LibraryFilter { ALL, SCANNED, PDF, AUDIOBOOKS, FAVORITES }

@Composable
fun LibraryRoute(
    onOpen: (String) -> Unit,
    onBack: () -> Unit,
    onScan: () -> Unit = {},
    onImportPdf: () -> Unit = {},
    showBack: Boolean = true,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val books by viewModel.booksState.collectAsStateWithLifecycle()
    LibraryScreen(books, onOpen, onBack, viewModel::delete, showBack, onScan, onImportPdf)
}

@Composable
fun LibraryScreen(
    books: List<LibraryBook>,
    onOpen: (String) -> Unit,
    onBack: () -> Unit,
    onDelete: (String) -> Unit,
    showBack: Boolean = true,
    onScan: () -> Unit = {},
    onImportPdf: () -> Unit = {},
) {
    val brand = LocalBrand.current
    val dimens = LocalDimens.current
    var filter by remember { mutableStateOf(LibraryFilter.ALL) }
    var query by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    val visible = books.filter { book ->
        val matchesFilter = when (filter) {
            LibraryFilter.ALL -> true
            LibraryFilter.SCANNED -> book.book.sourceType == BookSource.CAMERA_SCAN || book.book.sourceType == BookSource.MIXED
            LibraryFilter.PDF -> book.book.sourceType == BookSource.PDF || book.book.sourceType == BookSource.MIXED
            LibraryFilter.AUDIOBOOKS -> book.progressPercent > 0
            LibraryFilter.FAVORITES -> false
        }
        val matchesQuery = query.isBlank() || book.book.title.contains(query, ignoreCase = true)
        matchesFilter && matchesQuery
    }
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = brand.background,
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = dimens.screen, vertical = 8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("My Library", style = MaterialTheme.typography.displaySmall, color = brand.textPrimary, modifier = Modifier.weight(1f))
                IconButton(onClick = { showSearch = !showSearch }) {
                    Icon(Icons.Outlined.Search, contentDescription = "Search", tint = brand.textPrimary)
                }
                IconButton(onClick = { }) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "More options", tint = brand.textPrimary)
                }
            }
            if (showSearch) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    placeholder = { Text("Search books") },
                    singleLine = true,
                )
            }
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChipItem("All", filter == LibraryFilter.ALL) { filter = LibraryFilter.ALL }
                FilterChipItem("Scanned", filter == LibraryFilter.SCANNED) { filter = LibraryFilter.SCANNED }
                FilterChipItem("PDF", filter == LibraryFilter.PDF) { filter = LibraryFilter.PDF }
                FilterChipItem("Audiobooks", filter == LibraryFilter.AUDIOBOOKS) { filter = LibraryFilter.AUDIOBOOKS }
                FilterChipItem("Favorites", filter == LibraryFilter.FAVORITES) { filter = LibraryFilter.FAVORITES }
            }
            if (books.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        title = "Your library is empty",
                        message = "Scan a book or import a PDF to get started.",
                        icon = Icons.Outlined.AutoStories,
                        primary = "Scan Book",
                        onPrimary = onScan,
                        secondary = "Import PDF",
                        onSecondary = onImportPdf,
                    )
                }
            } else if (visible.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        title = "No books here",
                        message = "Try another filter or search.",
                        icon = Icons.Outlined.AutoStories,
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(visible, key = { it.book.id }) { item ->
                        LibraryBookCard(item, onOpen = { onOpen(item.book.id) }, onDelete = { onDelete(item.book.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryBookCard(item: LibraryBook, onOpen: () -> Unit, onDelete: () -> Unit) {
    val brand = LocalBrand.current
    var menu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    BookReaderCard(onClick = onOpen) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    Modifier.size(width = 52.dp, height = 64.dp).clip(RoundedCornerShape(10.dp)).background(brand.surfaceSecondary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Description, null, tint = brand.textSecondary, modifier = Modifier.size(24.dp))
                }
                IconButton(onClick = { confirmDelete = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Delete ${item.book.title}", tint = brand.textSecondary, modifier = Modifier.size(18.dp))
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(item.book.title, style = MaterialTheme.typography.titleMedium, color = brand.textPrimary)
                Text(item.book.language.displayName, style = MaterialTheme.typography.bodySmall, color = brand.textSecondary)
                Text("${item.completedPages} / ${item.book.totalPages} pages", style = MaterialTheme.typography.bodySmall, color = brand.textSecondary)
                Text("${item.progressPercent}%", style = MaterialTheme.typography.bodySmall, color = brand.textSecondary)
                ReaderProgressBar(item.progressPercent)
            }
            Box {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "Book options", tint = brand.textSecondary)
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = { menu = false; confirmDelete = true },
                        leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                    )
                }
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${item.book.title}?") },
            text = { Text("Its pages, text, and cached speech are removed from this phone.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}
