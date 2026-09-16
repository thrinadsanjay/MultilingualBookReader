package com.multilingualbookreader.presentation.library

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.multilingualbookreader.backup.BookBackup
import com.multilingualbookreader.domain.model.BookPriority
import com.multilingualbookreader.domain.model.LibraryBook
import com.multilingualbookreader.domain.repository.BookRepository
import com.multilingualbookreader.domain.usecase.ObserveLibraryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class LibraryEvent {
    data class ShareFile(val file: File, val title: String) : LibraryEvent()
    data class ShareText(val text: String, val title: String) : LibraryEvent()
    data class Message(val text: String) : LibraryEvent()
}

@HiltViewModel
class LibraryViewModel @Inject constructor(
    observeLibrary: ObserveLibraryUseCase,
    private val books: BookRepository,
    private val backup: BookBackup,
) : ViewModel() {
    val booksState = observeLibrary().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<LibraryBook>())
    private val _events = MutableSharedFlow<LibraryEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<LibraryEvent> = _events

    fun delete(bookId: String) {
        viewModelScope.launch { books.deleteBook(bookId) }
    }

    fun rename(bookId: String, title: String) = update(bookId) { it.copy(title = title.trim().ifBlank { it.title }) }

    fun setTags(bookId: String, tags: List<String>) = update(bookId) { it.copy(tags = tags.map { tag -> tag.trim() }.filter { tag -> tag.isNotEmpty() }.distinct()) }

    fun setPriority(bookId: String, priority: BookPriority) = update(bookId) { it.copy(priority = priority) }

    fun setColor(bookId: String, color: String) = update(bookId) { it.copy(color = color) }

    fun setGenre(bookId: String, genre: String) = update(bookId) { it.copy(genre = genre) }

    fun toggleFavorite(bookId: String) = update(bookId) { it.copy(favorite = !it.favorite) }

    fun share(bookId: String) {
        viewModelScope.launch {
            runCatching { backup.shareText(bookId) }
                .onSuccess { text ->
                    val title = books.getBook(bookId)?.title ?: "Svara"
                    _events.emit(LibraryEvent.ShareText(text, title))
                }
                .onFailure { _events.emit(LibraryEvent.Message(it.message ?: "Could not share this book.")) }
        }
    }

    fun export(bookId: String) {
        viewModelScope.launch {
            runCatching { backup.exportBook(bookId) }
                .onSuccess { file ->
                    val title = books.getBook(bookId)?.title ?: "Svara book"
                    _events.emit(LibraryEvent.ShareFile(file, title))
                }
                .onFailure { _events.emit(LibraryEvent.Message(it.message ?: "Could not export this book.")) }
        }
    }

    fun importBook(uri: Uri) {
        viewModelScope.launch {
            runCatching { backup.importBook(uri) }
                .onSuccess { _events.emit(LibraryEvent.Message("Book imported.")) }
                .onFailure { _events.emit(LibraryEvent.Message(it.message ?: "Could not import that file.")) }
        }
    }

    private fun update(bookId: String, transform: (com.multilingualbookreader.domain.model.Book) -> com.multilingualbookreader.domain.model.Book) {
        viewModelScope.launch {
            val book = books.getBook(bookId) ?: return@launch
            books.upsertBook(transform(book).copy(updatedAt = System.currentTimeMillis()))
        }
    }
}
