package com.multilingualbookreader.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey val id: String,
    val title: String,
    val author: String?,
    val coverPath: String?,
    val sourceType: String,
    val language: String,
    val totalPages: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val tags: String = "",
    val priority: String = "NORMAL",
    val color: String = "",
    val genre: String = "",
    val favorite: Boolean = false,
)

@Entity(
    tableName = "book_pages",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("bookId"), Index(value = ["bookId", "pageNumber"], unique = true)],
)
data class BookPageEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val pageNumber: Int,
    val imagePath: String?,
    val text: String,
    val language: String,
    val processingStatus: String,
    val errorMessage: String?,
    val hasSelectableText: Boolean,
)

@Entity(
    tableName = "book_text_blocks",
    foreignKeys = [
        ForeignKey(
            entity = BookPageEntity::class,
            parentColumns = ["id"],
            childColumns = ["pageId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("pageId")],
)
data class BookTextBlockEntity(
    @PrimaryKey val id: String,
    val pageId: String,
    val sequence: Int,
    val text: String,
    val language: String,
    val startOffset: Int,
    val endOffset: Int,
    val boxLeft: Float?,
    val boxTop: Float?,
    val boxRight: Float?,
    val boxBottom: Float?,
)

@Entity(tableName = "reading_progress")
data class ReadingProgressEntity(
    @PrimaryKey val bookId: String,
    val pageNumber: Int,
    val segmentId: String?,
    val characterOffset: Int,
    val updatedAt: Long,
)

@Entity(
    tableName = "audio_segments",
    indices = [Index("bookPageId"), Index("cacheKey", unique = true), Index("voiceProfileId")],
)
data class AudioSegmentEntity(
    @PrimaryKey val id: String,
    val bookPageId: String,
    val sequence: Int,
    val textHash: String,
    val language: String,
    val audioPath: String?,
    val durationMs: Long,
    val voiceProfileId: String,
    val status: String,
    val cacheKey: String,
)

@Entity(tableName = "voice_profiles")
data class VoiceProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val provider: String,
    val providerVoiceId: String?,
    val supportedLanguages: String,
    val experimentalLanguages: String,
    val status: String,
    val isCloned: Boolean,
    val qualityNote: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "bookmarks",
    indices = [Index("bookId")],
)
data class BookmarkEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val pageNumber: Int,
    val characterOffset: Int,
    val createdAt: Long,
)

@Entity(
    tableName = "notes",
    indices = [Index("bookId")],
)
data class NoteEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val pageNumber: Int,
    val selectedText: String,
    val body: String,
    val startOffset: Int,
    val endOffset: Int,
    val createdAt: Long,
    val updatedAt: Long,
)
