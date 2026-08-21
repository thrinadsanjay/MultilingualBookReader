package com.multilingualbookreader.presentation.scan

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.multilingualbookreader.camera.PageImageProcessor
import com.multilingualbookreader.domain.engine.OcrEngine
import com.multilingualbookreader.domain.model.Book
import com.multilingualbookreader.domain.model.BookPage
import com.multilingualbookreader.domain.model.BookSource
import com.multilingualbookreader.domain.model.OcrResult
import com.multilingualbookreader.domain.model.ProcessingStatus
import com.multilingualbookreader.domain.model.SupportedLanguage
import com.multilingualbookreader.domain.repository.BookRepository
import com.multilingualbookreader.storage.LocalFileStore
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.ByteArrayOutputStream
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ScanUiState(
    val bookId: String? = null,
    val pageCount: Int = 0,
    val preview: Bitmap? = null,
    val ocrText: String = "",
    val language: SupportedLanguage = SupportedLanguage.UNKNOWN,
    val busy: Boolean = false,
    val error: String? = null,
    val blurry: Boolean = false,
)

@HiltViewModel
class ScanViewModel @Inject constructor(
    application: Application,
    savedStateHandle: SavedStateHandle,
    private val ocr: OcrEngine,
    private val books: BookRepository,
    private val files: LocalFileStore,
) : AndroidViewModel(application) {
    private val initialBookId = savedStateHandle.get<String>("bookId").orEmpty().ifBlank { null }
    private val _state = MutableStateFlow(ScanUiState(bookId = initialBookId))
    val state: StateFlow<ScanUiState> = _state

    fun onCaptured(bytes: ByteArray) {
        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true, error = null)
            runCatching {
                withContext(Dispatchers.Default) {
                    val decoded = PageImageProcessor.decode(bytes)
                    val cropped = PageImageProcessor.cropToFrame(decoded, 0.08f, 0.12f, 0.92f, 0.88f)
                    val enhanced = PageImageProcessor.enhanceContrast(cropped)
                    val blurry = PageImageProcessor.blurScore(enhanced) < 6.0
                    val jpeg = ByteArrayOutputStream().apply { enhanced.compress(Bitmap.CompressFormat.JPEG, 90, this) }.toByteArray()
                    val result: OcrResult = ocr.recognize(jpeg)
                    _state.value = _state.value.copy(
                        preview = enhanced,
                        ocrText = result.text,
                        language = result.language,
                        busy = false,
                        blurry = blurry,
                    )
                }
            }.onFailure {
                _state.value = _state.value.copy(busy = false, error = "We could not read this page. Try a clearer photo.")
            }
        }
    }

    fun updateText(text: String) {
        _state.value = _state.value.copy(ocrText = text)
    }

    fun savePage() {
        viewModelScope.launch {
            val current = _state.value
            val bitmap = current.preview ?: return@launch
            val bookId = current.bookId ?: createBook()
            val pages = books.getPages(bookId)
            val pageNumber = pages.size + 1
            val jpeg = ByteArrayOutputStream().apply { bitmap.compress(Bitmap.CompressFormat.JPEG, 88, this) }.toByteArray()
            val imagePath = files.savePageImage(bookId, pageNumber, jpeg)
            books.upsertPage(
                BookPage(
                    id = UUID.randomUUID().toString(),
                    bookId = bookId,
                    pageNumber = pageNumber,
                    imagePath = imagePath,
                    text = current.ocrText,
                    language = current.language,
                    processingStatus = ProcessingStatus.COMPLETED,
                ),
            )
            val book = books.getBook(bookId)!!
            books.upsertBook(
                book.copy(
                    totalPages = pageNumber,
                    coverPath = book.coverPath ?: imagePath,
                    language = current.language,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
            _state.value = ScanUiState(bookId = bookId, pageCount = pageNumber)
        }
    }

    fun retake() {
        _state.value = _state.value.copy(preview = null, ocrText = "", error = null, blurry = false)
    }

    private suspend fun createBook(): String {
        val id = UUID.randomUUID().toString()
        books.upsertBook(
            Book(
                id = id,
                title = "Scanned book",
                author = null,
                coverPath = null,
                sourceType = BookSource.CAMERA_SCAN,
                language = SupportedLanguage.UNKNOWN,
                totalPages = 0,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
            ),
        )
        _state.value = _state.value.copy(bookId = id)
        return id
    }
}
