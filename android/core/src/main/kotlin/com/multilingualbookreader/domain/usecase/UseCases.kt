package com.multilingualbookreader.domain.usecase

import com.multilingualbookreader.domain.model.AppSettings
import com.multilingualbookreader.domain.model.BookPage
import com.multilingualbookreader.domain.model.LibraryBook
import com.multilingualbookreader.domain.model.ReadingProgress
import com.multilingualbookreader.domain.model.SupportedLanguage
import com.multilingualbookreader.domain.repository.BookRepository
import com.multilingualbookreader.domain.repository.ProgressRepository
import com.multilingualbookreader.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

class ObserveLibraryUseCase(private val books: BookRepository) {
    operator fun invoke(): Flow<List<LibraryBook>> = books.observeLibrary()
}

class GetContinueReadingUseCase(
    private val progress: ProgressRepository,
) {
    suspend operator fun invoke(): LibraryBook? = progress.continueReading()
}

class SaveReadingProgressUseCase(
    private val progress: ProgressRepository,
) {
    suspend operator fun invoke(value: ReadingProgress) = progress.save(value)
}

class SearchBookUseCase(private val books: BookRepository) {
    suspend operator fun invoke(bookId: String, query: String): List<BookPage> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()
        return books.search(bookId, trimmed)
    }
}

class ObserveSettingsUseCase(private val settings: SettingsRepository) {
    operator fun invoke(): Flow<AppSettings> = settings.observe()
}

class DetectPrimaryLanguageUseCase {
    operator fun invoke(pages: List<BookPage>): SupportedLanguage {
        val counts = pages.groupingBy { it.language }.eachCount()
            .filterKeys { it != SupportedLanguage.UNKNOWN && it != SupportedLanguage.MIXED }
        return counts.maxByOrNull { it.value }?.key ?: SupportedLanguage.MIXED
    }
}
