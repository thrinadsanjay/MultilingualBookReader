package com.multilingualbookreader.database

import com.multilingualbookreader.domain.model.AudioSegment
import com.multilingualbookreader.domain.model.AudioStatus
import com.multilingualbookreader.domain.model.Book
import com.multilingualbookreader.domain.model.BookPage
import com.multilingualbookreader.domain.model.BookPriority
import com.multilingualbookreader.domain.model.BookSource
import com.multilingualbookreader.domain.model.Bookmark
import com.multilingualbookreader.domain.model.Note
import com.multilingualbookreader.domain.model.ProcessingStatus
import com.multilingualbookreader.domain.model.ReadingProgress
import com.multilingualbookreader.domain.model.SupportedLanguage
import com.multilingualbookreader.domain.model.VoiceProfile
import com.multilingualbookreader.domain.model.VoiceStatus

fun BookEntity.toDomain() = Book(
    id = id,
    title = title,
    author = author,
    coverPath = coverPath,
    sourceType = BookSource.valueOf(sourceType),
    language = SupportedLanguage.fromBcp47(language),
    totalPages = totalPages,
    createdAt = createdAt,
    updatedAt = updatedAt,
    tags = tags.split(",").map { it.trim() }.filter { it.isNotEmpty() },
    priority = BookPriority.fromStored(priority),
    color = color,
    genre = genre,
    favorite = favorite,
)

fun Book.toEntity() = BookEntity(
    id = id,
    title = title,
    author = author,
    coverPath = coverPath,
    sourceType = sourceType.name,
    language = language.bcp47,
    totalPages = totalPages,
    createdAt = createdAt,
    updatedAt = updatedAt,
    tags = tags.joinToString(","),
    priority = priority.name,
    color = color,
    genre = genre,
    favorite = favorite,
)

fun BookPageEntity.toDomain() = BookPage(
    id = id,
    bookId = bookId,
    pageNumber = pageNumber,
    imagePath = imagePath,
    text = text,
    language = SupportedLanguage.fromBcp47(language),
    processingStatus = ProcessingStatus.valueOf(processingStatus),
    errorMessage = errorMessage,
    hasSelectableText = hasSelectableText,
)

fun BookPage.toEntity() = BookPageEntity(
    id = id,
    bookId = bookId,
    pageNumber = pageNumber,
    imagePath = imagePath,
    text = text,
    language = language.bcp47,
    processingStatus = processingStatus.name,
    errorMessage = errorMessage,
    hasSelectableText = hasSelectableText,
)

fun ReadingProgressEntity.toDomain() = ReadingProgress(
    bookId = bookId,
    pageNumber = pageNumber,
    segmentId = segmentId,
    characterOffset = characterOffset,
    updatedAt = updatedAt,
)

fun ReadingProgress.toEntity() = ReadingProgressEntity(
    bookId = bookId,
    pageNumber = pageNumber,
    segmentId = segmentId,
    characterOffset = characterOffset,
    updatedAt = updatedAt,
)

fun VoiceProfileEntity.toDomain() = VoiceProfile(
    id = id,
    name = name,
    provider = provider,
    providerVoiceId = providerVoiceId,
    supportedLanguages = supportedLanguages.split(",").filter { it.isNotBlank() }.map { SupportedLanguage.fromBcp47(it) },
    experimentalLanguages = experimentalLanguages.split(",").filter { it.isNotBlank() }.map { SupportedLanguage.fromBcp47(it) },
    status = VoiceStatus.valueOf(status),
    isCloned = isCloned,
    qualityNote = qualityNote,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun VoiceProfile.toEntity() = VoiceProfileEntity(
    id = id,
    name = name,
    provider = provider,
    providerVoiceId = providerVoiceId,
    supportedLanguages = supportedLanguages.joinToString(",") { it.bcp47 },
    experimentalLanguages = experimentalLanguages.joinToString(",") { it.bcp47 },
    status = status.name,
    isCloned = isCloned,
    qualityNote = qualityNote,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun BookmarkEntity.toDomain() = Bookmark(id, bookId, pageNumber, characterOffset, createdAt)
fun Bookmark.toEntity() = BookmarkEntity(id, bookId, pageNumber, characterOffset, createdAt)
fun NoteEntity.toDomain() = Note(id, bookId, pageNumber, selectedText, body, startOffset, endOffset, createdAt, updatedAt)
fun Note.toEntity() = NoteEntity(id, bookId, pageNumber, selectedText, body, startOffset, endOffset, createdAt, updatedAt)

fun AudioSegmentEntity.toDomain() = AudioSegment(
    id = id,
    bookPageId = bookPageId,
    sequence = sequence,
    text = "",
    language = SupportedLanguage.fromBcp47(language),
    audioPath = audioPath,
    durationMs = durationMs,
    voiceProfileId = voiceProfileId,
    status = AudioStatus.valueOf(status),
    cacheKey = cacheKey,
)
