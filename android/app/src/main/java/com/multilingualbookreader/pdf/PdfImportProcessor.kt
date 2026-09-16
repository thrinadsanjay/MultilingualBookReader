package com.multilingualbookreader.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.multilingualbookreader.camera.PageImageProcessor
import com.multilingualbookreader.common.AppLog
import com.multilingualbookreader.domain.engine.LanguageDetector
import com.multilingualbookreader.domain.engine.OcrEngine
import com.multilingualbookreader.domain.engine.TextProcessor
import com.multilingualbookreader.domain.model.Book
import com.multilingualbookreader.domain.model.BookLooks
import com.multilingualbookreader.domain.model.BookPage
import com.multilingualbookreader.domain.model.BookSource
import com.multilingualbookreader.domain.model.ProcessingStatus
import com.multilingualbookreader.domain.model.SupportedLanguage
import com.multilingualbookreader.domain.repository.BookRepository
import com.multilingualbookreader.storage.LocalFileStore
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

data class PdfImportProgress(
    val bookId: String,
    val currentPage: Int,
    val totalPages: Int,
    val message: String,
)

@Singleton
class PdfImportProcessor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val books: BookRepository,
    private val files: LocalFileStore,
    private val ocr: OcrEngine,
    private val textProcessor: TextProcessor,
    private val languageDetector: LanguageDetector,
) {
    private val _progress = MutableStateFlow<PdfImportProgress?>(null)
    val progress: StateFlow<PdfImportProgress?> = _progress

    suspend fun importPdf(displayName: String, pdfBytes: ByteArray): String = withContext(Dispatchers.IO) {
        PDFBoxResourceLoader.init(context)
        val bookId = UUID.randomUUID().toString()
        val pdfPath = files.savePdfCopy(displayName, pdfBytes)
        val pdfFile = File(pdfPath)
        val pageCount = pageCount(pdfFile)
        val title = displayName.removeSuffix(".pdf").ifBlank { "Imported book" }
        val book = Book(
            id = bookId,
            title = title,
            author = null,
            coverPath = null,
            sourceType = BookSource.PDF,
            language = SupportedLanguage.UNKNOWN,
            totalPages = pageCount,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            color = BookLooks.colorFor(bookId),
        )
        books.upsertBook(book)
        var languageVotes = mutableMapOf<SupportedLanguage, Int>()
        ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                PDDocument.load(pdfFile).use { document ->
                    for (index in 0 until pageCount) {
                        val pageNumber = index + 1
                        _progress.value = PdfImportProgress(bookId, pageNumber, pageCount, "Processing book...")
                        val pageId = UUID.randomUUID().toString()
                        books.upsertPage(
                            BookPage(
                                id = pageId,
                                bookId = bookId,
                                pageNumber = pageNumber,
                                imagePath = null,
                                text = "",
                                language = SupportedLanguage.UNKNOWN,
                                processingStatus = ProcessingStatus.PROCESSING,
                            ),
                        )
                        val extracted = extractPageText(document, pageNumber)
                        val page = if (extracted.trim().length >= MIN_SELECTABLE_CHARS) {
                            val cleaned = textProcessor.clean(extracted)
                            val language = languageDetector.detect(cleaned)
                            languageVotes[language] = (languageVotes[language] ?: 0) + 1
                            BookPage(
                                id = pageId,
                                bookId = bookId,
                                pageNumber = pageNumber,
                                imagePath = null,
                                text = cleaned,
                                language = language,
                                processingStatus = ProcessingStatus.COMPLETED,
                                hasSelectableText = true,
                            )
                        } else {
                            val imageBytes = renderPage(renderer, index)
                            val imagePath = files.savePageImage(bookId, pageNumber, imageBytes)
                            if (book.coverPath == null && pageNumber == 1) {
                                books.upsertBook(book.copy(coverPath = imagePath))
                            }
                            val attempt = runCatching { ocr.recognize(imageBytes) }
                            val ocrResult = attempt.getOrNull()
                            if (ocrResult == null) {
                                AppLog.w("pdf_ocr_failed", mapOf("page" to pageNumber))
                                books.upsertPage(
                                    BookPage(
                                        id = pageId,
                                        bookId = bookId,
                                        pageNumber = pageNumber,
                                        imagePath = imagePath,
                                        text = "",
                                        language = SupportedLanguage.UNKNOWN,
                                        processingStatus = ProcessingStatus.FAILED,
                                        errorMessage = readableOcrError(attempt.exceptionOrNull()),
                                    ),
                                )
                                null
                            } else {
                                val cleaned = textProcessor.clean(ocrResult.text, ocrResult.language)
                                languageVotes[ocrResult.language] = (languageVotes[ocrResult.language] ?: 0) + 1
                                BookPage(
                                    id = pageId,
                                    bookId = bookId,
                                    pageNumber = pageNumber,
                                    imagePath = imagePath,
                                    text = cleaned,
                                    language = ocrResult.language,
                                    processingStatus = ProcessingStatus.COMPLETED,
                                    hasSelectableText = false,
                                )
                            }
                        }
                        if (page != null) books.upsertPage(page)
                    }
                }
            }
        }
        val primary = languageVotes.maxByOrNull { it.value }?.key ?: SupportedLanguage.UNKNOWN
        books.upsertBook(book.copy(language = primary, coverPath = books.getBook(bookId)?.coverPath, updatedAt = System.currentTimeMillis()))
        _progress.value = null
        AppLog.i("pdf_import_complete", mapOf("pages" to pageCount, "language" to primary.bcp47))
        bookId
    }

    private fun pageCount(file: File): Int {
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { return it.pageCount }
        }
    }

    private fun extractPageText(document: PDDocument, pageNumber: Int): String {
        val stripper = PDFTextStripper().apply {
            startPage = pageNumber
            endPage = pageNumber
            sortByPosition = true
        }
        return stripper.getText(document).orEmpty()
    }

    private fun renderPage(renderer: PdfRenderer, index: Int): ByteArray {
        renderer.openPage(index).use { page ->
            val width = (page.width * 2).coerceAtMost(1600)
            val height = (page.height * width) / page.width
            // Must be white before rendering: PdfRenderer leaves untouched areas transparent and
            // JPEG has no alpha, so an un-erased page saves as black text on black.
            val bitmap = PageImageProcessor.newPageCanvas(width, height)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            bitmap.recycle()
            return out.toByteArray()
        }
    }

    companion object {
        private const val MIN_SELECTABLE_CHARS = 40

        /** Says what actually went wrong, so a failed page is diagnosable instead of just blank. */
        fun readableOcrError(error: Throwable?): String {
            val detail = error?.message?.trim().orEmpty()
            return when {
                detail.isEmpty() -> "This page could not be read."
                detail.contains("offline", ignoreCase = true) ->
                    "This page needs the server to be read, and the phone is offline."
                else -> "This page could not be read: ${detail.take(120)}"
            }
        }
    }
}
