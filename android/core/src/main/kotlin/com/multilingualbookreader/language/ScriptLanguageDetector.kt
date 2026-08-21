package com.multilingualbookreader.language

import com.multilingualbookreader.domain.engine.LanguageDetector
import com.multilingualbookreader.domain.model.LanguageSpan
import com.multilingualbookreader.domain.model.SupportedLanguage

/**
 * Script-based detector. This is deterministic and works fully offline.
 * It is the source of truth for routing TTS and mixed-language splitting.
 */
class ScriptLanguageDetector : LanguageDetector {

    override fun detect(text: String): SupportedLanguage {
        val spans = detectSpans(text)
        if (spans.isEmpty()) return SupportedLanguage.UNKNOWN
        val counts = spans.groupingBy { it.language }.fold(0) { acc, span -> acc + span.text.length }
        val meaningful = counts.filterKeys { it != SupportedLanguage.UNKNOWN }
        if (meaningful.isEmpty()) return SupportedLanguage.UNKNOWN
        if (meaningful.size > 1) return SupportedLanguage.MIXED
        return meaningful.maxBy { it.value }.key
    }

    override fun detectSpans(text: String): List<LanguageSpan> {
        if (text.isEmpty()) return emptyList()
        val spans = mutableListOf<LanguageSpan>()
        var start = 0
        var current = languageOf(text[0])
        for (index in 1 until text.length) {
            val next = languageOf(text[index])
            val merged = mergeLanguage(current, next)
            if (merged != current && next != SupportedLanguage.UNKNOWN && current != SupportedLanguage.UNKNOWN) {
                spans += LanguageSpan(text.substring(start, index), current, start, index)
                start = index
                current = next
            } else if (current == SupportedLanguage.UNKNOWN) {
                current = next
            }
        }
        spans += LanguageSpan(text.substring(start), current, start, text.length)
        return spans.filter { it.text.isNotBlank() }
    }

    private fun mergeLanguage(current: SupportedLanguage, next: SupportedLanguage): SupportedLanguage {
        if (next == SupportedLanguage.UNKNOWN) return current
        if (current == SupportedLanguage.UNKNOWN) return next
        return if (current == next) current else next
    }

    private fun languageOf(char: Char): SupportedLanguage {
        val code = char.code
        return when {
            char.isWhitespace() || isSharedPunctuation(char) -> SupportedLanguage.UNKNOWN
            code in TELUGU_RANGE -> SupportedLanguage.TELUGU
            code in DEVANAGARI_RANGE -> SupportedLanguage.HINDI
            char.isLetter() && code < 0x0250 -> SupportedLanguage.ENGLISH
            else -> SupportedLanguage.UNKNOWN
        }
    }

    private fun isSharedPunctuation(char: Char): Boolean {
        return char in ".,;:!?\"'()[]{}-—–/\\|@#$%^&*_+=<>~`।॥"
    }

    companion object {
        private val TELUGU_RANGE = 0x0C00..0x0C7F
        private val DEVANAGARI_RANGE = 0x0900..0x097F
    }
}
