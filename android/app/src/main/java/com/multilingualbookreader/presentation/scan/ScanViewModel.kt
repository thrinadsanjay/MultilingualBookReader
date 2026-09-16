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
import com.multilingualbookreader.domain.model.BookLooks
import com.multilingualbookreader.domain.model.BookPage
import com.multilingualbookreader.domain.model.BookSource
import com.multilingualbookreader.domain.model.OcrResult
import com.multilingualbookreader.domain.model.ProcessingStatus
import com.multilingualbookreader.domain.model.SupportedLanguage
import com.multilingualbookreader.domain.repository.BookRepository
import com.multilingualbookreader.ocr.OcrFallbackPolicy
import com.multilingualbookreader.storage.LocalFileStore
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ScanCaptureMode { SINGLE, MULTIPLE, BOOK }

enum class ScanFlash { OFF, AUTO, ON }

data class PageDraft(
    val id: String,
    val source: Bitmap,
    val rotationDegrees: Int = 0,
    val cropInset: Float = 0f,
    val enhance: Boolean = false,
    val preview: Bitmap,
) {
    fun edited(
        rotationDegrees: Int = this.rotationDegrees,
        cropInset: Float = this.cropInset,
        enhance: Boolean = this.enhance,
    ): PageDraft {
        val rendered = PageImageProcessor.renderDraft(source, rotationDegrees, cropInset, enhance)
        return copy(
            rotationDegrees = rotationDegrees,
            cropInset = cropInset,
            enhance = enhance,
            preview = rendered,
        )
    }

    companion object {
        fun from(source: Bitmap): PageDraft {
            val id = UUID.randomUUID().toString()
            return PageDraft(id = id, source = source, preview = source)
        }
    }
}

data class ScanUiState(
    val bookId: String? = null,
    val pageCount: Int = 0,
    val drafts: List<PageDraft> = emptyList(),
    val selectedDraftIndex: Int = 0,
    val preview: Bitmap? = null,
    val ocrText: String = "",
    val language: SupportedLanguage = SupportedLanguage.UNKNOWN,
    val busy: Boolean = false,
    val busyMessage: String? = null,
    val error: String? = null,
    val blurry: Boolean = false,
    val mode: ScanCaptureMode = ScanCaptureMode.SINGLE,
    val flash: ScanFlash = ScanFlash.AUTO,
    val autoCrop: Boolean = true,
    val highQuality: Boolean = false,
    val showTips: Boolean = false,
    val openReaderId: String? = null,
    val bookTitle: String = "",
    val askForTitle: Boolean = false,
    val pendingOpenReaderId: String? = null,
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

    init {
        val existingId = initialBookId
        if (existingId != null) {
            viewModelScope.launch {
                val book = books.getBook(existingId)
                val count = books.getPages(existingId).size
                _state.value = _state.value.copy(
                    pageCount = count,
                    mode = ScanCaptureMode.MULTIPLE,
                    bookTitle = book?.title.orEmpty(),
                )
            }
        }
    }

    fun onCaptured(bytes: ByteArray) {
        val crop = _state.value.autoCrop && _state.value.mode != ScanCaptureMode.BOOK
        viewModelScope.launch { importBytes(listOf(bytes), cropToCameraFrame = crop) }
    }

    fun setMode(mode: ScanCaptureMode) {
        _state.value = _state.value.copy(mode = mode)
    }

    fun cycleFlash() {
        val next = when (_state.value.flash) {
            ScanFlash.OFF -> ScanFlash.AUTO
            ScanFlash.AUTO -> ScanFlash.ON
            ScanFlash.ON -> ScanFlash.OFF
        }
        _state.value = _state.value.copy(flash = next)
    }

    fun toggleAutoCrop() {
        _state.value = _state.value.copy(autoCrop = !_state.value.autoCrop)
    }

    fun toggleHighQuality() {
        _state.value = _state.value.copy(highQuality = !_state.value.highQuality)
    }

    fun toggleTips() {
        _state.value = _state.value.copy(showTips = !_state.value.showTips)
    }

    fun consumeOpenReader() {
        _state.value = _state.value.copy(openReaderId = null)
    }

    fun setBookTitle(title: String) {
        _state.value = _state.value.copy(bookTitle = title)
    }

    fun confirmTitle(title: String) {
        viewModelScope.launch {
            val current = _state.value
            val id = current.bookId ?: return@launch
            val name = title.trim().ifBlank { "Untitled book" }
            val book = books.getBook(id) ?: return@launch
            books.upsertBook(book.copy(title = name, updatedAt = System.currentTimeMillis()))
            _state.value = current.copy(
                bookTitle = name,
                askForTitle = false,
                pendingOpenReaderId = null,
                openReaderId = current.pendingOpenReaderId ?: current.openReaderId,
            )
        }
    }

    fun skipTitle() {
        val current = _state.value
        _state.value = current.copy(
            askForTitle = false,
            pendingOpenReaderId = null,
            openReaderId = current.pendingOpenReaderId ?: current.openReaderId,
        )
    }

    fun onGalleryPicked(uri: Uri) = onGalleryPicked(listOf(uri))

    fun onGalleryPicked(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true, error = null, busyMessage = "Loading photos…")
            runCatching {
                val pages = withContext(Dispatchers.IO) {
                    uris.map { uri ->
                        val bytes = getApplication<Application>().contentResolver.openInputStream(uri)?.use { it.readBytes() }
                            ?: error("Could not open that photo.")
                        PageImageProcessor.decodeForEditing(bytes, cropToCameraFrame = false)
                    }
                }
                appendDrafts(pages)
            }.onFailure(::failRead)
        }
    }

    fun selectDraft(index: Int) {
        val drafts = _state.value.drafts
        if (index !in drafts.indices) return
        _state.value = _state.value.copy(selectedDraftIndex = index, error = null)
    }

    fun rotateSelected() {
        updateSelected { it.edited(rotationDegrees = (it.rotationDegrees + 90) % 360) }
    }

    fun cycleCropSelected() {
        updateSelected { draft ->
            val next = when {
                draft.cropInset < 0.03f -> 0.06f
                draft.cropInset < 0.09f -> 0.12f
                draft.cropInset < 0.15f -> 0.18f
                else -> 0f
            }
            draft.edited(cropInset = next)
        }
    }

    fun toggleEnhanceSelected() {
        updateSelected { it.edited(enhance = !it.enhance) }
    }

    fun removeSelected() {
        val current = _state.value
        if (current.drafts.isEmpty()) return
        val remaining = current.drafts.filterIndexed { index, _ -> index != current.selectedDraftIndex }
        _state.value = current.copy(
            drafts = remaining,
            selectedDraftIndex = remaining.lastIndex.coerceAtLeast(0),
            error = null,
        )
    }

    fun cancelPrepare() {
        _state.value = _state.value.copy(drafts = emptyList(), selectedDraftIndex = 0, error = null, busy = false)
    }

    fun detectSelected() {
        val current = _state.value
        val draft = current.drafts.getOrNull(current.selectedDraftIndex) ?: return
        viewModelScope.launch {
            _state.value = current.copy(busy = true, error = null, busyMessage = "Reading this page…")
            runCatching { recognizeDraft(draft) }.onFailure(::failRead)
        }
    }

    fun detectAll() {
        val drafts = _state.value.drafts
        if (drafts.isEmpty()) return
        viewModelScope.launch {
            val start = _state.value
            if (start.busy) return@launch
            _state.value = start.copy(busy = true, error = null)
            runCatching {
                var bookId = start.bookId
                var pageCount = start.pageCount
                drafts.forEachIndexed { index, draft ->
                    _state.value = _state.value.copy(busyMessage = "Reading page ${index + 1} of ${drafts.size}…")
                    val recognized = withContext(Dispatchers.Default) { ocrPage(draft) }
                    val persisted = withContext(Dispatchers.IO + NonCancellable) {
                    persistPage(
                        bookId = bookId,
                        bitmap = recognized.bitmap,
                        text = recognized.text,
                        language = recognized.language,
                        titleHint = start.bookTitle,
                    )
                    }
                    bookId = persisted.first
                    pageCount = persisted.second
                }
                val needsName = start.bookId == null && start.bookTitle.isBlank()
                _state.value = start.copy(
                    bookId = bookId,
                    pageCount = pageCount,
                    drafts = emptyList(),
                    selectedDraftIndex = 0,
                    preview = null,
                    ocrText = "",
                    busy = false,
                    busyMessage = null,
                    error = null,
                    blurry = false,
                    askForTitle = needsName,
                    pendingOpenReaderId = bookId.takeIf { needsName && start.mode == ScanCaptureMode.SINGLE },
                    openReaderId = bookId.takeIf { !needsName && start.mode == ScanCaptureMode.SINGLE },
                )
            }.onFailure(::failRead)
        }
    }

    fun updateText(text: String) {
        _state.value = _state.value.copy(ocrText = text)
    }

    fun savePage() {
        viewModelScope.launch {
            val current = _state.value
            val bitmap = current.preview ?: return@launch
            if (current.busy) return@launch
            _state.value = current.copy(busy = true, error = null, busyMessage = "Saving page…")
            runCatching {
                withContext(Dispatchers.IO + NonCancellable) {
                    persistPage(
                        bookId = current.bookId,
                        bitmap = bitmap,
                        text = current.ocrText,
                        language = current.language,
                        titleHint = current.bookTitle,
                    )
                }
            }.onSuccess { (bookId, pageNumber) ->
                val remaining = current.drafts.filterIndexed { index, _ -> index != current.selectedDraftIndex }
                val needsName = current.bookId == null && current.bookTitle.isBlank()
                _state.value = current.copy(
                    bookId = bookId,
                    pageCount = pageNumber,
                    drafts = remaining,
                    selectedDraftIndex = remaining.lastIndex.coerceAtLeast(0),
                    preview = null,
                    ocrText = "",
                    busy = false,
                    busyMessage = null,
                    error = null,
                    blurry = false,
                    askForTitle = needsName,
                    pendingOpenReaderId = bookId.takeIf { needsName && current.mode == ScanCaptureMode.SINGLE && remaining.isEmpty() },
                    openReaderId = bookId.takeIf { !needsName && current.mode == ScanCaptureMode.SINGLE && remaining.isEmpty() },
                )
            }.onFailure { error ->
                _state.value = current.copy(
                    bookId = _state.value.bookId ?: current.bookId,
                    busy = false,
                    busyMessage = null,
                    error = "This page could not be saved. ${error.message?.take(90) ?: "Try again."}",
                )
            }
        }
    }

    fun retake() {
        _state.value = _state.value.copy(preview = null, ocrText = "", error = null, blurry = false, busyMessage = null)
    }

    private fun updateSelected(transform: (PageDraft) -> PageDraft) {
        val current = _state.value
        val index = current.selectedDraftIndex
        val draft = current.drafts.getOrNull(index) ?: return
        val updated = current.drafts.toMutableList().also { it[index] = transform(draft) }
        _state.value = current.copy(drafts = updated, error = null)
    }

    private suspend fun importBytes(pages: List<ByteArray>, cropToCameraFrame: Boolean) {
        _state.value = _state.value.copy(busy = true, error = null, busyMessage = "Preparing page…")
        runCatching {
            val bitmaps = withContext(Dispatchers.Default) {
                pages.map { PageImageProcessor.decodeForEditing(it, cropToCameraFrame) }
            }
            appendDrafts(bitmaps)
        }.onFailure(::failRead)
    }

    private fun appendDrafts(pages: List<Bitmap>) {
        if (pages.isEmpty()) {
            _state.value = _state.value.copy(busy = false, busyMessage = null)
            return
        }
        val current = _state.value
        val added = pages.map(PageDraft::from)
        val drafts = current.drafts + added
        _state.value = current.copy(
            drafts = drafts,
            selectedDraftIndex = current.drafts.size,
            preview = null,
            ocrText = "",
            busy = false,
            busyMessage = null,
            error = null,
            blurry = false,
        )
    }

    private suspend fun recognizeDraft(draft: PageDraft) {
        val recognized = withContext(Dispatchers.Default) { ocrPage(draft) }
        _state.value = _state.value.copy(
            preview = recognized.bitmap,
            ocrText = recognized.text,
            language = recognized.language,
            busy = false,
            busyMessage = null,
            blurry = recognized.blurry,
            error = recognized.warning,
        )
    }

    private suspend fun ocrPage(draft: PageDraft): RecognizedDraft {
        val bitmap = draft.preview
        val jpeg = PageImageProcessor.toJpeg(bitmap)
        val result: OcrResult = ocr.recognize(jpeg)
        val warning = when {
            result.text.isBlank() -> "We could not read any text. Rotate until the lines read left to right, then try again."
                OcrFallbackPolicy.looksUnreliable(result.text) ->
                    "The text still looks off. Rotate until the writing is upright, then detect again. A reading server in Settings can help on difficult Telugu pages."
            else -> null
        }
        return RecognizedDraft(bitmap, result.text, result.language, PageImageProcessor.blurScore(bitmap) < 6.0, warning)
    }

    private fun failRead(error: Throwable) {
        _state.value = _state.value.copy(
            busy = false,
            busyMessage = null,
            error = "We could not read this page. Try a clearer photo. (${error.message?.take(90) ?: "unknown error"})",
        )
    }

    private suspend fun persistPage(
        bookId: String?,
        bitmap: Bitmap,
        text: String,
        language: SupportedLanguage,
        titleHint: String,
    ): Pair<String, Int> {
        val id = bookId ?: UUID.randomUUID().toString()
        val existing = books.getBook(id)
        val pageNumber = books.getPages(id).size + 1
        val jpeg = PageImageProcessor.toJpeg(bitmap, 88)
        if (jpeg.isEmpty()) error("Could not write the photo.")
        val imagePath = files.savePageImage(id, pageNumber, jpeg)
        val now = System.currentTimeMillis()
        val book = (existing ?: Book(
            id = id,
            title = titleHint.trim().ifBlank { "Scanned book" },
            author = null,
            coverPath = null,
            sourceType = BookSource.CAMERA_SCAN,
            language = SupportedLanguage.UNKNOWN,
            totalPages = 0,
            createdAt = now,
            updatedAt = now,
            color = BookLooks.colorFor(id),
        )).copy(
            totalPages = pageNumber,
            coverPath = existing?.coverPath ?: imagePath,
            language = language,
            updatedAt = now,
        )
        books.saveBookPage(
            book,
            BookPage(
                id = UUID.randomUUID().toString(),
                bookId = id,
                pageNumber = pageNumber,
                imagePath = imagePath,
                text = text,
                language = language,
                processingStatus = ProcessingStatus.COMPLETED,
            ),
        )
        check(books.getPages(id).any { it.pageNumber == pageNumber }) {
            "The page was not stored."
        }
        return id to pageNumber
    }

    private data class RecognizedDraft(
        val bitmap: Bitmap,
        val text: String,
        val language: SupportedLanguage,
        val blurry: Boolean,
        val warning: String?,
    )
}
