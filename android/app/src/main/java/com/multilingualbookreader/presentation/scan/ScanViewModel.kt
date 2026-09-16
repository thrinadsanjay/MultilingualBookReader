package com.multilingualbookreader.presentation.scan

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
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
import com.multilingualbookreader.ocr.OcrFallbackPolicy
import com.multilingualbookreader.storage.LocalFileStore
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.ByteArrayOutputStream
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
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

    fun onCaptured(bytes: ByteArray) = recognizePage(bytes, cropToCameraFrame = true)

    fun onGalleryPicked(uri: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true, error = null)
            runCatching {
                val bytes = withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: error("Could not open that photo.")
                }
                recognizeLoadedPage(bytes, cropToCameraFrame = false)
            }.onFailure(::failRead)
        }
    }

    private fun recognizePage(bytes: ByteArray, cropToCameraFrame: Boolean) {
        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true, error = null)
            runCatching { recognizeLoadedPage(bytes, cropToCameraFrame) }.onFailure(::failRead)
        }
    }

    private suspend fun recognizeLoadedPage(bytes: ByteArray, cropToCameraFrame: Boolean) {
        withContext(Dispatchers.Default) {
            val prepared = PageImageProcessor.prepareForOcr(bytes, cropToCameraFrame)
            val result: OcrResult = ocr.recognize(prepared.jpeg)
            val warning = when {
                result.text.isBlank() -> "We could not read any text. Try a flatter, brighter photo."
                OcrFallbackPolicy.looksUnreliable(result.text) ->
                    "The text still looks off. If the photo is sideways, retake it. Telugu pages need the reading server in Settings."
                else -> null
            }
            _state.value = _state.value.copy(
                preview = prepared.bitmap,
                ocrText = result.text,
                language = result.language,
                busy = false,
                blurry = prepared.blurry,
                error = warning,
            )
        }
    }

    private fun failRead(error: Throwable) {
        _state.value = _state.value.copy(
            busy = false,
            error = "We could not read this page. Try a clearer photo. (${error.message?.take(90) ?: "unknown error"})",
        )
    }

    fun updateText(text: String) {
        _state.value = _state.value.copy(ocrText = text)
    }

    fun savePage() {
        viewModelScope.launch {
            val current = _state.value
            val bitmap = current.preview ?: return@launch
            if (current.busy) return@launch
            _state.value = current.copy(busy = true, error = null)
            runCatching {
                withContext(Dispatchers.IO + NonCancellable) {
                    persistPage(current, bitmap)
                }
            }.onSuccess { (bookId, pageNumber) ->
                _state.value = ScanUiState(bookId = bookId, pageCount = pageNumber)
            }.onFailure { error ->
                _state.value = current.copy(
                    bookId = _state.value.bookId ?: current.bookId,
                    busy = false,
                    error = "This page could not be saved. ${error.message?.take(90) ?: "Try again."}",
                )
            }
        }
    }

    private suspend fun persistPage(current: ScanUiState, bitmap: Bitmap): Pair<String, Int> {
        val bookId = current.bookId ?: createBook()
        val pageNumber = books.getPages(bookId).size + 1
        val jpeg = ByteArrayOutputStream().apply { bitmap.compress(Bitmap.CompressFormat.JPEG, 88, this) }.toByteArray()
        if (jpeg.isEmpty()) error("Could not write the photo.")
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
        val book = books.getBook(bookId) ?: error("The book disappeared before the page was saved.")
        books.upsertBook(
            book.copy(
                totalPages = pageNumber,
                coverPath = book.coverPath ?: imagePath,
                language = current.language,
                updatedAt = System.currentTimeMillis(),
            ),
        )
        return bookId to pageNumber
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
