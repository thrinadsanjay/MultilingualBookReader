package com.multilingualbookreader.domain.model

data class Book(
    val id: String,
    val title: String,
    val author: String?,
    val coverPath: String?,
    val sourceType: BookSource,
    val language: SupportedLanguage,
    val totalPages: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val tags: List<String> = emptyList(),
    val priority: BookPriority = BookPriority.NORMAL,
    val color: String = "",
    val genre: String = "",
    val favorite: Boolean = false,
)

data class BookPage(
    val id: String,
    val bookId: String,
    val pageNumber: Int,
    val imagePath: String?,
    val text: String,
    val language: SupportedLanguage,
    val processingStatus: ProcessingStatus,
    val errorMessage: String? = null,
    val hasSelectableText: Boolean = false,
)

data class BookTextBlock(
    val id: String,
    val pageId: String,
    val sequence: Int,
    val text: String,
    val language: SupportedLanguage,
    val startOffset: Int,
    val endOffset: Int,
    val boundingBox: BoundingBox? = null,
)

data class ReadingProgress(
    val bookId: String,
    val pageNumber: Int,
    val segmentId: String?,
    val characterOffset: Int,
    val updatedAt: Long,
)

data class AudioSegment(
    val id: String,
    val bookPageId: String,
    val sequence: Int,
    val text: String,
    val language: SupportedLanguage,
    val audioPath: String?,
    val durationMs: Long,
    val voiceProfileId: String,
    val status: AudioStatus,
    val cacheKey: String,
)

data class VoiceProfile(
    val id: String,
    val name: String,
    val provider: String,
    val providerVoiceId: String?,
    val supportedLanguages: List<SupportedLanguage>,
    val experimentalLanguages: List<SupportedLanguage> = emptyList(),
    val status: VoiceStatus,
    val isCloned: Boolean,
    val qualityNote: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

data class Bookmark(
    val id: String,
    val bookId: String,
    val pageNumber: Int,
    val characterOffset: Int,
    val createdAt: Long,
)

data class Note(
    val id: String,
    val bookId: String,
    val pageNumber: Int,
    val selectedText: String,
    val body: String,
    val startOffset: Int,
    val endOffset: Int,
    val createdAt: Long,
    val updatedAt: Long,
)

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val fontScale: Float = 1.0f,
    val highContrast: Boolean = false,
    val reduceMotion: Boolean = true,
    val playbackSpeed: Float = 1.0f,
    val selectedVoiceId: String? = null,
    val ocrRoute: OcrRoute = OcrRoute.AUTO,
    val largeControls: Boolean = true,
    val analyticsEnabled: Boolean = false,
    val crashReportingEnabled: Boolean = false,
    val backendBaseUrl: String = "",
    val defaultLanguageTag: String = "AUTO",
)

data class LibraryBook(
    val book: Book,
    val completedPages: Int,
    val progressPercent: Int,
    val lastReadAt: Long?,
)
