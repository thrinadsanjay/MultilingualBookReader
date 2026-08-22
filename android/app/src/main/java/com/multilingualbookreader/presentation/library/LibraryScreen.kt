package com.multilingualbookreader.presentation.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.multilingualbookreader.domain.model.LibraryBook
import com.multilingualbookreader.presentation.components.ScreenHeader

@Composable
fun LibraryRoute(
    onOpen: (String) -> Unit,
    onBack: () -> Unit,
    showBack: Boolean = true,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val books by viewModel.booksState.collectAsStateWithLifecycle()
    LibraryScreen(books, onOpen, onBack, viewModel::delete, showBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    books: List<LibraryBook>,
    onOpen: (String) -> Unit,
    onBack: () -> Unit,
    onDelete: (String) -> Unit,
    showBack: Boolean = true,
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("My Library") },
                navigationIcon = {
                    if (showBack) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (books.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
                ScreenHeader("My Library", "Imported and scanned books will appear here.")
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(books, key = { it.book.id }) { item ->
                    BookCard(item, onOpen = { onOpen(item.book.id) }, onDelete = { onDelete(item.book.id) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookCard(item: LibraryBook, onOpen: () -> Unit, onDelete: () -> Unit) {
    Card(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(item.book.title, style = MaterialTheme.typography.titleLarge)
            Text(item.book.language.displayName, style = MaterialTheme.typography.bodyLarge)
            Text("${item.completedPages} / ${item.book.totalPages} pages")
            LinearProgressIndicator(
                progress = { item.progressPercent / 100f },
                modifier = Modifier.fillMaxWidth(),
            )
            Text("${item.progressPercent}%")
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete book")
            }
        }
    }
}
