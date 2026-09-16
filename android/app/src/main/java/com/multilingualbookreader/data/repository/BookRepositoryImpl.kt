package com.multilingualbookreader.data.repository

import androidx.room.withTransaction
import com.multilingualbookreader.database.BookDao
import com.multilingualbookreader.database.BookPageDao
import com.multilingualbookreader.database.BookReaderDatabase
import com.multilingualbookreader.database.ProgressDao
import com.multilingualbookreader.database.toDomain
import com.multilingualbookreader.database.toEntity
import com.multilingualbookreader.domain.model.Book
import com.multilingualbookreader.domain.model.BookPage
import com.multilingualbookreader.domain.model.LibraryBook
import com.multilingualbookreader.domain.model.ProcessingStatus
import com.multilingualbookreader.domain.repository.BookRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

@Singleton
class BookRepositoryImpl @Inject constructor(
    private val db: BookReaderDatabase,
    private val bookDao: BookDao,
    private val pageDao: BookPageDao,
    private val progressDao: ProgressDao,
) : BookRepository {
    override fun observeLibrary(): Flow<List<LibraryBook>> = flow {
        bookDao.observeBooks().collect { books ->
            emit(
                books.map { entity ->
                    val pages = pageDao.getPages(entity.id)
                    val completed = pages.count {
                        it.processingStatus == ProcessingStatus.COMPLETED.name && it.text.isNotBlank()
                    }
                    val progress = progressDao.get(entity.id)
                    val percent = if (entity.totalPages == 0) {
                        0
                    } else {
                        ((progress?.pageNumber ?: 0) * 100 / entity.totalPages)
                    }
                    LibraryBook(
                        book = entity.toDomain(),
                        completedPages = completed,
                        progressPercent = percent.coerceIn(0, 100),
                        lastReadAt = progress?.updatedAt,
                    )
                },
            )
        }
    }

    override fun observeBook(bookId: String) = bookDao.observeBook(bookId).map { it?.toDomain() }
    override fun observePages(bookId: String) = pageDao.observePages(bookId).map { pages -> pages.map { it.toDomain() } }
    override suspend fun getBook(bookId: String) = bookDao.getBook(bookId)?.toDomain()
    override suspend fun getPages(bookId: String) = pageDao.getPages(bookId).map { it.toDomain() }
    override suspend fun getPage(pageId: String) = pageDao.getPage(pageId)?.toDomain()
    override suspend fun upsertBook(book: Book) = bookDao.upsert(book.toEntity())
    override suspend fun upsertPage(page: BookPage) = pageDao.upsert(page.toEntity())
    override suspend fun saveBookPage(book: Book, page: BookPage) {
        db.withTransaction {
            bookDao.upsert(book.toEntity())
            pageDao.upsert(page.toEntity())
        }
    }
    override suspend fun deleteBook(bookId: String) {
        pageDao.deleteForBook(bookId)
        bookDao.delete(bookId)
    }
    override suspend fun search(bookId: String, query: String) = pageDao.search(bookId, query).map { it.toDomain() }
    override suspend fun updatePageText(pageId: String, text: String) {
        pageDao.updateText(pageId, text, ProcessingStatus.COMPLETED.name)
    }
}
