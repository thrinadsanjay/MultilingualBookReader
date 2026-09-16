package com.multilingualbookreader.backup

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.multilingualbookreader.data.repository.BookRepositoryImpl
import com.multilingualbookreader.database.BookReaderDatabase
import com.multilingualbookreader.domain.model.Book
import com.multilingualbookreader.domain.model.BookPage
import com.multilingualbookreader.domain.model.BookPriority
import com.multilingualbookreader.domain.model.BookSource
import com.multilingualbookreader.domain.model.ProcessingStatus
import com.multilingualbookreader.domain.model.SupportedLanguage
import com.multilingualbookreader.storage.LocalFileStore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BookBackupTest {
    private lateinit var db: BookReaderDatabase
    private lateinit var books: BookRepositoryImpl
    private lateinit var backup: BookBackup

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        db = Room.inMemoryDatabaseBuilder(context, BookReaderDatabase::class.java).allowMainThreadQueries().build()
        books = BookRepositoryImpl(db, db.bookDao(), db.pageDao(), db.progressDao())
        backup = BookBackup(context, books, LocalFileStore(context))
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun exportThenImportRestoresTitleTagsAndText() = runBlocking {
        val book = Book(
            id = "src",
            title = "యథాతథము",
            author = null,
            coverPath = null,
            sourceType = BookSource.CAMERA_SCAN,
            language = SupportedLanguage.TELUGU,
            totalPages = 1,
            createdAt = 1,
            updatedAt = 1,
            tags = listOf("Telugu"),
            priority = BookPriority.HIGH,
            genre = "Religion",
        )
        books.saveBookPage(
            book,
            BookPage(
                id = "p1",
                bookId = "src",
                pageNumber = 1,
                imagePath = null,
                text = "నమస్కారం",
                language = SupportedLanguage.TELUGU,
                processingStatus = ProcessingStatus.COMPLETED,
            ),
        )
        val zip = backup.exportBook("src")
        val importedId = backup.importBytes(zip.readBytes())
        val imported = books.getBook(importedId)!!
        assertThat(imported.title).isEqualTo("యథాతథము")
        assertThat(imported.tags).contains("Telugu")
        assertThat(imported.priority).isEqualTo(BookPriority.HIGH)
        assertThat(imported.genre).isEqualTo("Religion")
        assertThat(books.getPages(importedId).single().text).isEqualTo("నమస్కారం")
    }
}
