package com.multilingualbookreader.domain.repository

import com.multilingualbookreader.domain.model.AppSettings
import com.multilingualbookreader.domain.model.AudioSegment
import com.multilingualbookreader.domain.model.Book
import com.multilingualbookreader.domain.model.BookPage
import com.multilingualbookreader.domain.model.Bookmark
import com.multilingualbookreader.domain.model.LibraryBook
import com.multilingualbookreader.domain.model.Note
import com.multilingualbookreader.domain.model.ReadingProgress
import com.multilingualbookreader.domain.model.VoiceProfile
import kotlinx.coroutines.flow.Flow

interface BookRepository {
    fun observeLibrary(): Flow<List<LibraryBook>>
    fun observeBook(bookId: String): Flow<Book?>
    fun observePages(bookId: String): Flow<List<BookPage>>
    suspend fun getBook(bookId: String): Book?
    suspend fun getPages(bookId: String): List<BookPage>
    suspend fun getPage(pageId: String): BookPage?
    suspend fun upsertBook(book: Book)
    suspend fun upsertPage(page: BookPage)
    suspend fun saveBookPage(book: Book, page: BookPage)
    suspend fun deleteBook(bookId: String)
    suspend fun search(bookId: String, query: String): List<BookPage>
    suspend fun updatePageText(pageId: String, text: String)
}

interface ProgressRepository {
    fun observe(bookId: String): Flow<ReadingProgress?>
    suspend fun get(bookId: String): ReadingProgress?
    suspend fun save(progress: ReadingProgress)
    suspend fun continueReading(): LibraryBook?
}

interface BookmarkRepository {
    fun observe(bookId: String): Flow<List<Bookmark>>
    suspend fun add(bookmark: Bookmark)
    suspend fun remove(id: String)
}

interface NoteRepository {
    fun observe(bookId: String): Flow<List<Note>>
    suspend fun upsert(note: Note)
    suspend fun remove(id: String)
}

interface VoiceRepository {
    fun observeProfiles(): Flow<List<VoiceProfile>>
    suspend fun get(id: String): VoiceProfile?
    suspend fun upsert(profile: VoiceProfile)
    suspend fun delete(id: String)
}

interface SettingsRepository {
    fun observe(): Flow<AppSettings>
    suspend fun get(): AppSettings
    suspend fun update(transform: (AppSettings) -> AppSettings)
}

interface AudioCacheRepository {
    suspend fun get(cacheKey: String): AudioSegment?
    suspend fun put(segment: AudioSegment)
    suspend fun deleteForVoice(voiceId: String)
    suspend fun deleteForBook(bookId: String)
}
