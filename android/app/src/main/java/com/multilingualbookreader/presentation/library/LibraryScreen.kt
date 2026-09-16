package com.multilingualbookreader.presentation.library

import android.content.Intent
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.multilingualbookreader.camera.PageImageProcessor
import com.multilingualbookreader.domain.model.BookLooks
import com.multilingualbookreader.domain.model.BookPriority
import com.multilingualbookreader.domain.model.BookSource
import com.multilingualbookreader.domain.model.LibraryBook
import com.multilingualbookreader.presentation.components.BookReaderCard
import com.multilingualbookreader.presentation.components.EmptyState
import com.multilingualbookreader.presentation.components.FilterChipItem
import com.multilingualbookreader.presentation.components.ReaderProgressBar
import com.multilingualbookreader.presentation.theme.LocalBrand
import com.multilingualbookreader.presentation.theme.LocalDimens
import java.util.Date

enum class LibraryFilter { ALL, SCANNED, PDF, HIGH, FAVORITES }

enum class LibrarySort { NEWEST, OLDEST, TITLE, PRIORITY, GENRE }

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
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::importBook)
    }
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is LibraryEvent.ShareFile -> {
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", event.file)
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "application/zip"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_SUBJECT, event.title)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(send, "Export ${event.title}"))
                }
                is LibraryEvent.ShareText -> {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, event.title)
                        putExtra(Intent.EXTRA_TEXT, event.text)
                    }
                    context.startActivity(Intent.createChooser(send, "Share ${event.title}"))
                }
                is LibraryEvent.Message -> snackbar.showSnackbar(event.text)
            }
        }
    }
    LibraryScreen(
        books = books,
        onOpen = onOpen,
        onBack = onBack,
        onDelete = viewModel::delete,
        onRename = viewModel::rename,
        onTags = viewModel::setTags,
        onPriority = viewModel::setPriority,
        onColor = viewModel::setColor,
        onGenre = viewModel::setGenre,
        onFavorite = viewModel::toggleFavorite,
        onShare = viewModel::share,
        onExport = viewModel::export,
        onImport = { importer.launch(arrayOf("application/zip", "*/*")) },
        showBack = showBack,
        onScan = onScan,
        onImportPdf = onImportPdf,
        snackbar = snackbar,
    )
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
    onRename: (String, String) -> Unit = { _, _ -> },
    onTags: (String, List<String>) -> Unit = { _, _ -> },
    onPriority: (String, BookPriority) -> Unit = { _, _ -> },
    onColor: (String, String) -> Unit = { _, _ -> },
    onGenre: (String, String) -> Unit = { _, _ -> },
    onFavorite: (String) -> Unit = {},
    onShare: (String) -> Unit = {},
    onExport: (String) -> Unit = {},
    onImport: () -> Unit = {},
    snackbar: SnackbarHostState = remember { SnackbarHostState() },
) {
    val brand = LocalBrand.current
    val dimens = LocalDimens.current
    var filter by remember { mutableStateOf(LibraryFilter.ALL) }
    var sort by remember { mutableStateOf(LibrarySort.NEWEST) }
    var query by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    var sortMenu by remember { mutableStateOf(false) }
    var topMenu by remember { mutableStateOf(false) }
    var genreFilter by remember { mutableStateOf<String?>(null) }
    val genresInLibrary = remember(books) {
        books.map { it.book.genre }.filter { it.isNotBlank() }.distinct().sorted()
    }
    val visible = books.filter { book ->
        val matchesFilter = when (filter) {
            LibraryFilter.ALL -> true
            LibraryFilter.SCANNED -> book.book.sourceType == BookSource.CAMERA_SCAN || book.book.sourceType == BookSource.MIXED
            LibraryFilter.PDF -> book.book.sourceType == BookSource.PDF || book.book.sourceType == BookSource.MIXED
            LibraryFilter.HIGH -> book.book.priority == BookPriority.HIGH || book.book.priority == BookPriority.URGENT
            LibraryFilter.FAVORITES -> book.book.favorite
        }
        val matchesGenre = genreFilter == null || book.book.genre == genreFilter
        val haystack = buildString {
            append(book.book.title)
            append(' ')
            append(book.book.genre)
            append(' ')
            append(book.book.tags.joinToString(" "))
        }
        val matchesQuery = query.isBlank() || haystack.contains(query, ignoreCase = true)
        matchesFilter && matchesGenre && matchesQuery
    }.sortedWith(
        when (sort) {
            LibrarySort.NEWEST -> compareByDescending { it.book.createdAt }
            LibrarySort.OLDEST -> compareBy { it.book.createdAt }
            LibrarySort.TITLE -> compareBy { it.book.title.lowercase() }
            LibrarySort.PRIORITY -> compareByDescending<LibraryBook> { it.book.priority.ordinal }.thenByDescending { it.book.createdAt }
            LibrarySort.GENRE -> compareBy<LibraryBook> { it.book.genre.ifBlank { "\uFFFF" }.lowercase() }.thenBy { it.book.title.lowercase() }
        },
    )
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = brand.background,
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = dimens.screen, vertical = 8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("My Library", style = MaterialTheme.typography.displaySmall, color = brand.textPrimary, modifier = Modifier.weight(1f))
                IconButton(onClick = { showSearch = !showSearch }) {
                    Icon(Icons.Outlined.Search, contentDescription = "Search", tint = brand.textPrimary)
                }
                Box {
                    IconButton(onClick = { sortMenu = true }) {
                        Icon(Icons.AutoMirrored.Outlined.Sort, contentDescription = "Sort", tint = brand.textPrimary)
                    }
                    DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                        DropdownMenuItem(text = { Text("Newest first") }, onClick = { sort = LibrarySort.NEWEST; sortMenu = false })
                        DropdownMenuItem(text = { Text("Oldest first") }, onClick = { sort = LibrarySort.OLDEST; sortMenu = false })
                        DropdownMenuItem(text = { Text("Title") }, onClick = { sort = LibrarySort.TITLE; sortMenu = false })
                        DropdownMenuItem(text = { Text("Priority") }, onClick = { sort = LibrarySort.PRIORITY; sortMenu = false })
                        DropdownMenuItem(text = { Text("Genre") }, onClick = { sort = LibrarySort.GENRE; sortMenu = false })
                    }
                }
                Box {
                    IconButton(onClick = { topMenu = true }) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "More options", tint = brand.textPrimary)
                    }
                    DropdownMenu(expanded = topMenu, onDismissRequest = { topMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Import book") },
                            onClick = { topMenu = false; onImport() },
                            leadingIcon = { Icon(Icons.Outlined.FileDownload, contentDescription = null) },
                        )
                    }
                }
            }
            if (showSearch) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    placeholder = { Text("Search title, tags, or genre") },
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
                FilterChipItem("Priority", filter == LibraryFilter.HIGH) { filter = LibraryFilter.HIGH }
                FilterChipItem("Favorites", filter == LibraryFilter.FAVORITES) { filter = LibraryFilter.FAVORITES }
            }
            if (genresInLibrary.isNotEmpty()) {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChipItem("All genres", genreFilter == null) { genreFilter = null }
                    genresInLibrary.forEach { genre ->
                        FilterChipItem(genre, genreFilter == genre) { genreFilter = genre }
                    }
                }
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
                        LibraryBookCard(
                            item = item,
                            onOpen = { onOpen(item.book.id) },
                            onDelete = { onDelete(item.book.id) },
                            onRename = { onRename(item.book.id, it) },
                            onTags = { onTags(item.book.id, it) },
                            onPriority = { onPriority(item.book.id, it) },
                            onColor = { onColor(item.book.id, it) },
                            onGenre = { onGenre(item.book.id, it) },
                            onFavorite = { onFavorite(item.book.id) },
                            onShare = { onShare(item.book.id) },
                            onExport = { onExport(item.book.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryBookCard(
    item: LibraryBook,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    onRename: (String) -> Unit,
    onTags: (List<String>) -> Unit,
    onPriority: (BookPriority) -> Unit,
    onColor: (String) -> Unit,
    onGenre: (String) -> Unit,
    onFavorite: () -> Unit,
    onShare: () -> Unit,
    onExport: () -> Unit,
) {
    val brand = LocalBrand.current
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var rename by remember { mutableStateOf(false) }
    var tags by remember { mutableStateOf(false) }
    var priority by remember { mutableStateOf(false) }
    var color by remember { mutableStateOf(false) }
    var genre by remember { mutableStateOf(false) }
    val accent = remember(item.book.color) { parseBookColor(item.book.color) ?: Color(0xFFE8A87C) }
    val cover = remember(item.book.coverPath) { item.book.coverPath?.let { PageImageProcessor.decodePageFile(it, 240) } }
    val added = remember(item.book.createdAt) {
        if (item.book.createdAt <= 0L) "Added date unknown" else "Added ${DateFormat.getMediumDateFormat(context).format(Date(item.book.createdAt))}"
    }
    BookReaderCard(onClick = onOpen) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier
                    .width(6.dp)
                    .height(92.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accent),
            )
            Box(
                Modifier
                    .size(width = 58.dp, height = 76.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                if (cover != null) {
                    Image(
                        bitmap = cover.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(Icons.Outlined.Description, null, tint = accent, modifier = Modifier.size(26.dp))
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.book.title, style = MaterialTheme.typography.titleMedium, color = brand.textPrimary, modifier = Modifier.weight(1f))
                    IconButton(onClick = onFavorite, modifier = Modifier.size(32.dp)) {
                        Icon(
                            if (item.book.favorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = if (item.book.favorite) "Remove favorite" else "Favorite",
                            tint = if (item.book.favorite) accent else brand.textSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Text(
                    listOfNotNull(
                        item.book.language.displayName.takeIf { it != "Unknown" },
                        sourceLabel(item.book.sourceType),
                        item.book.genre.takeIf { it.isNotBlank() },
                    ).joinToString(" · ").ifBlank { sourceLabel(item.book.sourceType) },
                    style = MaterialTheme.typography.bodySmall,
                    color = brand.textSecondary,
                )
                Text(added, style = MaterialTheme.typography.bodySmall, color = brand.textSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    PriorityChip(item.book.priority)
                    if (item.book.tags.isNotEmpty()) {
                        item.book.tags.take(3).forEach { tag ->
                            Text(
                                tag,
                                style = MaterialTheme.typography.labelMedium,
                                color = accent,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(accent.copy(alpha = 0.14f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
                Text("${item.completedPages} / ${item.book.totalPages} pages", style = MaterialTheme.typography.bodySmall, color = brand.textSecondary)
                ReaderProgressBar(item.progressPercent)
            }
            Box {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "Book options", tint = brand.textSecondary)
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Rename") }, onClick = { menu = false; rename = true }, leadingIcon = { Icon(Icons.Outlined.Edit, null) })
                    DropdownMenuItem(text = { Text("Tags") }, onClick = { menu = false; tags = true }, leadingIcon = { Icon(Icons.Outlined.Style, null) })
                    DropdownMenuItem(text = { Text("Priority") }, onClick = { menu = false; priority = true }, leadingIcon = { Icon(Icons.Outlined.Flag, null) })
                    DropdownMenuItem(text = { Text("Color") }, onClick = { menu = false; color = true }, leadingIcon = { Icon(Icons.Outlined.Palette, null) })
                    DropdownMenuItem(text = { Text("Genre") }, onClick = { menu = false; genre = true }, leadingIcon = { Icon(Icons.Outlined.AutoStories, null) })
                    DropdownMenuItem(text = { Text("Share") }, onClick = { menu = false; onShare() }, leadingIcon = { Icon(Icons.Outlined.Share, null) })
                    DropdownMenuItem(text = { Text("Export") }, onClick = { menu = false; onExport() }, leadingIcon = { Icon(Icons.Outlined.FileUpload, null) })
                    DropdownMenuItem(text = { Text("Delete") }, onClick = { menu = false; confirmDelete = true }, leadingIcon = { Icon(Icons.Outlined.Delete, null) })
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
    if (rename) {
        NameBookDialog(
            title = "Rename book",
            initial = item.book.title,
            confirm = "Save",
            onConfirm = { onRename(it); rename = false },
            onDismiss = { rename = false },
        )
    }
    if (tags) {
        NameBookDialog(
            title = "Tags",
            initial = item.book.tags.joinToString(", "),
            confirm = "Save",
            label = "Comma-separated tags",
            onConfirm = { onTags(it.split(",")); tags = false },
            onDismiss = { tags = false },
        )
    }
    if (priority) {
        ChoiceDialog(
            title = "Priority",
            options = BookPriority.entries.map { it.label to it.name },
            onPick = { name -> BookPriority.fromStored(name).let(onPriority); priority = false },
            onDismiss = { priority = false },
        )
    }
    if (genre) {
        ChoiceDialog(
            title = "Genre",
            options = (listOf("None" to "") + BookLooks.genres.map { it to it }),
            onPick = { onGenre(it); genre = false },
            onDismiss = { genre = false },
        )
    }
    if (color) {
        AlertDialog(
            onDismissRequest = { color = false },
            title = { Text("Color") },
            text = {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    BookLooks.colors.forEach { hex ->
                        val parsed = parseBookColor(hex) ?: return@forEach
                        Box(
                            Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(parsed)
                                .then(if (item.book.color == hex) Modifier.border(2.dp, brand.textPrimary, CircleShape) else Modifier)
                                .clickable { onColor(hex); color = false },
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { color = false }) { Text("Close") } },
        )
    }
}

@Composable
fun NameBookDialog(
    title: String,
    initial: String,
    confirm: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    label: String = "Name",
) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(label) },
                singleLine = true,
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(value) }) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ChoiceDialog(
    title: String,
    options: List<Pair<String, String>>,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { (label, value) ->
                    Text(
                        label,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(value) }
                            .padding(vertical = 10.dp),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun PriorityChip(priority: BookPriority) {
    val color = when (priority) {
        BookPriority.LOW -> Color(0xFF8AA36F)
        BookPriority.NORMAL -> Color(0xFF7EB6D9)
        BookPriority.HIGH -> Color(0xFFE8A87C)
        BookPriority.URGENT -> Color(0xFFE07A70)
    }
    Text(
        priority.label,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

private fun sourceLabel(source: BookSource) = when (source) {
    BookSource.CAMERA_SCAN -> "Scanned"
    BookSource.PDF -> "PDF"
    BookSource.MIXED -> "Mixed"
}

private fun parseBookColor(hex: String): Color? {
    val clean = hex.removePrefix("#")
    if (clean.length != 6) return null
    return runCatching { Color(android.graphics.Color.parseColor("#$clean")) }.getOrNull()
}
