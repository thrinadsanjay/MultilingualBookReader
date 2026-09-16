package com.multilingualbookreader.text

import com.multilingualbookreader.domain.engine.LanguageDetector
import com.multilingualbookreader.domain.model.SupportedLanguage
import com.multilingualbookreader.domain.model.TextSegment
import java.util.UUID

class SentenceSegmenter(
    private val languageDetector: LanguageDetector,
) {
    fun segment(pageText: String): List<TextSegment> {
        if (pageText.isBlank()) return emptyList()
        val pieces = SENTENCE_BOUNDARY.split(pageText)
        val segments = mutableListOf<TextSegment>()
        var cursor = 0
        for (piece in pieces) {
            val start = pageText.indexOf(piece, cursor).takeIf { it >= 0 } ?: cursor
            val end = start + piece.length
            cursor = end
            val text = piece.trim()
            if (text.isEmpty()) continue
            segments += TextSegment(
                id = UUID.randomUUID().toString(),
                text = text,
                language = languageDetector.detect(text),
                startOffset = start,
                endOffset = end.coerceAtMost(pageText.length),
            )
        }
        if (segments.isEmpty()) {
            segments += TextSegment(
                id = UUID.randomUUID().toString(),
                text = pageText.trim(),
                language = languageDetector.detect(pageText),
                startOffset = 0,
                endOffset = pageText.length,
            )
        }
        return segments
    }

    fun splitParagraphs(pageText: String): List<TextSegment> {
        val paragraphs = pageText.split(Regex("\\n\\s*\\n"))
        val segments = mutableListOf<TextSegment>()
        var cursor = 0
        paragraphs.forEach { paragraph ->
            val start = pageText.indexOf(paragraph, cursor).takeIf { it >= 0 } ?: cursor
            val end = start + paragraph.length
            cursor = end
            val text = paragraph.trim()
            if (text.isEmpty()) return@forEach
            segments += TextSegment(
                id = UUID.randomUUID().toString(),
                text = text,
                language = languageDetector.detect(text),
                startOffset = start,
                endOffset = end,
            )
        }
        return segments
    }

    companion object {
        private val SENTENCE_BOUNDARY = Regex("(?<=[.!?।॥])\\s+")
    }
}
