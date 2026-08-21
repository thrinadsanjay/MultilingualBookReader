package com.multilingualbookreader.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.multilingualbookreader.domain.model.BookPage
import com.multilingualbookreader.domain.usecase.SearchBookUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val results: List<BookPage> = emptyList(),
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val search: SearchBookUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state

    fun onQuery(bookId: String, query: String) {
        _state.value = _state.value.copy(query = query)
        viewModelScope.launch {
            _state.value = _state.value.copy(results = search(bookId, query))
        }
    }
}
