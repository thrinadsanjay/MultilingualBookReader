package com.multilingualbookreader.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.multilingualbookreader.domain.model.LibraryBook
import com.multilingualbookreader.domain.repository.BookRepository
import com.multilingualbookreader.domain.usecase.ObserveLibraryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class LibraryViewModel @Inject constructor(
    observeLibrary: ObserveLibraryUseCase,
    private val books: BookRepository,
) : ViewModel() {
    val booksState = observeLibrary().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<LibraryBook>())

    fun delete(bookId: String) {
        viewModelScope.launch { books.deleteBook(bookId) }
    }
}
