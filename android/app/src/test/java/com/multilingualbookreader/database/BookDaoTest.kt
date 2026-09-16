package com.multilingualbookreader.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BookDaoTest {
    private lateinit var db: BookReaderDatabase

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            BookReaderDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertAndReadBook() = runBlocking {
        db.bookDao().upsert(sampleBook())
        val loaded = db.bookDao().getBook("1")
        assertThat(loaded?.title).isEqualTo("నా కథలు")
        assertThat(loaded?.language).isEqualTo("te")
    }

    @Test
    fun updatingBookAfterInsertingPageKeepsThePage() = runBlocking {
        db.bookDao().upsert(sampleBook(totalPages = 0, coverPath = null))
        db.pageDao().upsert(samplePage())
        assertThat(db.pageDao().getPages("1")).hasSize(1)

        // Use Page used to INSERT OR REPLACE the book here, which CASCADE-deleted the page.
        db.bookDao().upsert(sampleBook(totalPages = 1, coverPath = "/pages/1.jpg", updatedAt = 2))

        val pages = db.pageDao().getPages("1")
        assertThat(pages).hasSize(1)
        assertThat(pages.single().text).isEqualTo("detected text")
        assertThat(db.bookDao().getBook("1")?.totalPages).isEqualTo(1)
        assertThat(db.bookDao().getBook("1")?.coverPath).isEqualTo("/pages/1.jpg")
    }

    private fun sampleBook(
        totalPages: Int = 12,
        coverPath: String? = null,
        updatedAt: Long = 1,
    ) = BookEntity(
        id = "1",
        title = "నా కథలు",
        author = null,
        coverPath = coverPath,
        sourceType = "PDF",
        language = "te",
        totalPages = totalPages,
        createdAt = 1,
        updatedAt = updatedAt,
    )

    private fun samplePage() = BookPageEntity(
        id = "page-1",
        bookId = "1",
        pageNumber = 1,
        imagePath = "/pages/1.jpg",
        text = "detected text",
        language = "te",
        processingStatus = "COMPLETED",
        errorMessage = null,
        hasSelectableText = false,
    )
}
