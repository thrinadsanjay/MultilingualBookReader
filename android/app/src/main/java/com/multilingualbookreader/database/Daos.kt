package com.multilingualbookreader.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY updatedAt DESC")
    fun observeBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :id")
    fun observeBook(id: String): Flow<BookEntity?>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getBook(id: String): BookEntity?

    // REPLACE deletes the row and CASCADE-wipes pages. @Upsert updates in place.
    @Upsert
    suspend fun upsert(book: BookEntity)

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface BookPageDao {
    @Query("SELECT * FROM book_pages WHERE bookId = :bookId ORDER BY pageNumber ASC")
    fun observePages(bookId: String): Flow<List<BookPageEntity>>

    @Query("SELECT * FROM book_pages WHERE bookId = :bookId ORDER BY pageNumber ASC")
    suspend fun getPages(bookId: String): List<BookPageEntity>

    @Query("SELECT * FROM book_pages WHERE id = :id")
    suspend fun getPage(id: String): BookPageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(page: BookPageEntity)

    @Query("UPDATE book_pages SET text = :text, processingStatus = :status WHERE id = :id")
    suspend fun updateText(id: String, text: String, status: String)

    @Query(
        """
        SELECT * FROM book_pages
        WHERE bookId = :bookId AND text LIKE '%' || :query || '%'
        ORDER BY pageNumber ASC
        """,
    )
    suspend fun search(bookId: String, query: String): List<BookPageEntity>

    @Query("SELECT COUNT(*) FROM book_pages WHERE bookId = :bookId AND processingStatus = 'COMPLETED'")
    suspend fun completedCount(bookId: String): Int

    @Query("DELETE FROM book_pages WHERE bookId = :bookId")
    suspend fun deleteForBook(bookId: String)
}

@Dao
interface TextBlockDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(blocks: List<BookTextBlockEntity>)

    @Query("SELECT * FROM book_text_blocks WHERE pageId = :pageId ORDER BY sequence ASC")
    suspend fun getForPage(pageId: String): List<BookTextBlockEntity>
}

@Dao
interface ProgressDao {
    @Query("SELECT * FROM reading_progress WHERE bookId = :bookId")
    fun observe(bookId: String): Flow<ReadingProgressEntity?>

    @Query("SELECT * FROM reading_progress WHERE bookId = :bookId")
    suspend fun get(bookId: String): ReadingProgressEntity?

    @Query("SELECT * FROM reading_progress ORDER BY updatedAt DESC LIMIT 1")
    suspend fun latest(): ReadingProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: ReadingProgressEntity)
}

@Dao
interface AudioSegmentDao {
    @Query("SELECT * FROM audio_segments WHERE cacheKey = :cacheKey LIMIT 1")
    suspend fun getByCacheKey(cacheKey: String): AudioSegmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(segment: AudioSegmentEntity)

    @Query("DELETE FROM audio_segments WHERE voiceProfileId = :voiceId")
    suspend fun deleteForVoice(voiceId: String)

    @Query(
        """
        DELETE FROM audio_segments WHERE bookPageId IN (
            SELECT id FROM book_pages WHERE bookId = :bookId
        )
        """,
    )
    suspend fun deleteForBook(bookId: String)
}

@Dao
interface VoiceProfileDao {
    @Query("SELECT * FROM voice_profiles WHERE status != 'DELETED' ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<VoiceProfileEntity>>

    @Query("SELECT * FROM voice_profiles WHERE id = :id")
    suspend fun get(id: String): VoiceProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: VoiceProfileEntity)

    @Query("DELETE FROM voice_profiles WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId ORDER BY pageNumber ASC")
    fun observe(bookId: String): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE bookId = :bookId ORDER BY pageNumber ASC")
    fun observe(bookId: String): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun delete(id: String)
}
