package com.multilingualbookreader.backup

import android.content.Context
import android.net.Uri
import com.multilingualbookreader.domain.model.Book
import com.multilingualbookreader.domain.model.BookLooks
import com.multilingualbookreader.domain.model.BookPage
import com.multilingualbookreader.domain.model.BookPriority
import com.multilingualbookreader.domain.model.BookSource
import com.multilingualbookreader.domain.model.ProcessingStatus
import com.multilingualbookreader.domain.model.SupportedLanguage
import com.multilingualbookreader.domain.repository.BookRepository
import com.multilingualbookreader.storage.LocalFileStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Singleton
class BookBackup @Inject constructor(
    @ApplicationContext private val context: Context,
    private val books: BookRepository,
    private val files: LocalFileStore,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun exportBook(bookId: String): File {
        val book = books.getBook(bookId) ?: error("That book is not on this phone.")
        val pages = books.getPages(bookId)
        val snapshot = BookSnapshot.from(book, pages)
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safe = book.title.replace(Regex("[^A-Za-z0-9._-]+"), "_").ifBlank { "book" }
        val out = File(dir, "$safe.svarabook.zip")
        ZipOutputStream(out.outputStream().buffered()).use { zip ->
            zip.putNextEntry(ZipEntry("book.json"))
            zip.write(json.encodeToString(BookSnapshot.serializer(), snapshot).toByteArray())
            zip.closeEntry()
            pages.forEach { page ->
                val path = page.imagePath ?: return@forEach
                val image = File(path)
                if (!image.isFile) return@forEach
                zip.putNextEntry(ZipEntry("pages/${page.pageNumber.toString().padStart(4, '0')}.jpg"))
                image.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
        return out
    }

    suspend fun shareText(bookId: String): String {
        val book = books.getBook(bookId) ?: error("That book is not on this phone.")
        val pages = books.getPages(bookId)
        return buildString {
            appendLine(book.title)
            if (book.genre.isNotBlank()) appendLine(book.genre)
            if (book.tags.isNotEmpty()) appendLine(book.tags.joinToString(", "))
            appendLine()
            pages.forEach { page ->
                appendLine("Page ${page.pageNumber}")
                appendLine(page.text.ifBlank { "(no text)" })
                appendLine()
            }
        }.trim()
    }

    suspend fun importBook(uri: Uri): String {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("Could not open that file.")
        return importBytes(bytes)
    }

    suspend fun importBytes(zipBytes: ByteArray): String {
        val images = mutableMapOf<Int, ByteArray>()
        var snapshot: BookSnapshot? = null
        ZipInputStream(zipBytes.inputStream()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val name = entry.name.trimStart('/')
                if (name == "book.json") {
                    snapshot = json.decodeFromString(BookSnapshot.serializer(), zip.readBytes().decodeToString())
                } else if (name.startsWith("pages/") && name.endsWith(".jpg")) {
                    val number = name.removePrefix("pages/").removeSuffix(".jpg").trimStart('0').ifBlank { "0" }.toIntOrNull()
                    if (number != null && number > 0) images[number] = zip.readBytes()
                }
                zip.closeEntry()
            }
        }
        val pack = snapshot ?: error("This file is not a Svara book backup.")
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val book = pack.toBook(id, now)
        books.upsertBook(book)
        var coverPath: String? = null
        pack.pages.sortedBy { it.pageNumber }.forEach { page ->
            val jpeg = images[page.pageNumber]
            val imagePath = jpeg?.let { files.savePageImage(id, page.pageNumber, it) }
            if (coverPath == null) coverPath = imagePath
            books.upsertPage(
                BookPage(
                    id = UUID.randomUUID().toString(),
                    bookId = id,
                    pageNumber = page.pageNumber,
                    imagePath = imagePath,
                    text = page.text,
                    language = SupportedLanguage.fromBcp47(page.language),
                    processingStatus = ProcessingStatus.COMPLETED,
                ),
            )
        }
        books.upsertBook(book.copy(coverPath = coverPath, totalPages = pack.pages.size, updatedAt = now))
        return id
    }
}

@Serializable
data class BookSnapshot(
    val title: String,
    val author: String? = null,
    val language: String = "und",
    val sourceType: String = "CAMERA_SCAN",
    val tags: List<String> = emptyList(),
    val priority: String = "NORMAL",
    val color: String = "",
    val genre: String = "",
    val pages: List<PageSnapshot> = emptyList(),
) {
    fun toBook(id: String, now: Long) = Book(
        id = id,
        title = title.ifBlank { "Imported book" },
        author = author,
        coverPath = null,
        sourceType = runCatching { BookSource.valueOf(sourceType) }.getOrDefault(BookSource.MIXED),
        language = SupportedLanguage.fromBcp47(language),
        totalPages = pages.size,
        createdAt = now,
        updatedAt = now,
        tags = tags,
        priority = BookPriority.fromStored(priority),
        color = color.ifBlank { BookLooks.colorFor(id) },
        genre = genre,
        favorite = false,
    )

    companion object {
        fun from(book: Book, pages: List<BookPage>) = BookSnapshot(
            title = book.title,
            author = book.author,
            language = book.language.bcp47,
            sourceType = book.sourceType.name,
            tags = book.tags,
            priority = book.priority.name,
            color = book.color,
            genre = book.genre,
            pages = pages.map {
                PageSnapshot(
                    pageNumber = it.pageNumber,
                    text = it.text,
                    language = it.language.bcp47,
                )
            },
        )
    }
}

@Serializable
data class PageSnapshot(
    val pageNumber: Int,
    val text: String = "",
    val language: String = "und",
)
