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
        db.bookDao().upsert(
            BookEntity(
                id = "1",
                title = "నా కథలు",
                author = null,
                coverPath = null,
                sourceType = "PDF",
                language = "te",
                totalPages = 12,
                createdAt = 1,
                updatedAt = 1,
            ),
        )
        val loaded = db.bookDao().getBook("1")
        assertThat(loaded?.title).isEqualTo("నా కథలు")
        assertThat(loaded?.language).isEqualTo("te")
    }
}
