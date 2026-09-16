package com.multilingualbookreader.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.multilingualbookreader.database.BookReaderDatabase
import com.multilingualbookreader.domain.model.Book
import com.multilingualbookreader.domain.model.BookPage
import com.multilingualbookreader.domain.model.BookSource
import com.multilingualbookreader.domain.model.ProcessingStatus
import com.multilingualbookreader.domain.model.SupportedLanguage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BookRepositoryImplTest {
    private lateinit var db: BookReaderDatabase
    private lateinit var books: BookRepositoryImpl

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            BookReaderDatabase::class.java,
        ).allowMainThreadQueries().build()
        books = BookRepositoryImpl(db, db.bookDao(), db.pageDao(), db.progressDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun usePageLeavesAReadablePageForTheReader() = runBlocking {
        val book = Book(
            id = "scan-1",
            title = "Scanned book",
            author = null,
            coverPath = "/pages/scan-1-0001.jpg",
            sourceType = BookSource.CAMERA_SCAN,
            language = SupportedLanguage.TELUGU,
            totalPages = 1,
            createdAt = 1,
            updatedAt = 1,
        )
        val page = BookPage(
            id = "page-1",
            bookId = "scan-1",
            pageNumber = 1,
            imagePath = "/pages/scan-1-0001.jpg",
            text = "నమస్కారం",
            language = SupportedLanguage.TELUGU,
            processingStatus = ProcessingStatus.COMPLETED,
        )

        books.saveBookPage(book, page)
        books.upsertBook(book.copy(coverPath = "/pages/scan-1-0001.jpg", updatedAt = 2))

        assertThat(books.getPages("scan-1")).hasSize(1)
        assertThat(books.observePages("scan-1").first().single().text).isEqualTo("నమస్కారం")
        assertThat(books.observeBook("scan-1").first()?.title).isEqualTo("Scanned book")
    }
}
