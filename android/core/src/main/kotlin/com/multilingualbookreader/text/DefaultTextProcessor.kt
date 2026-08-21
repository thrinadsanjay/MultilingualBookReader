package com.multilingualbookreader.text

import com.multilingualbookreader.domain.engine.TextProcessor
import com.multilingualbookreader.domain.model.SupportedLanguage

/**
 * Conservative OCR cleanup. We only repair layout artifacts that clearly
 * should not be spoken, and we never rewrite words in Indic scripts.
 */
class DefaultTextProcessor : TextProcessor {

    override fun clean(raw: String, language: SupportedLanguage): String {
        if (raw.isBlank()) return ""
        var text = raw.replace("\u0000", "")
        text = text.replace("\r\n", "\n").replace('\r', '\n')
        text = dropRepeatedHeaderFooter(text)
        text = joinHyphenatedLineBreaks(text)
        text = joinBrokenLines(text)
        text = normalizeWhitespace(text)
        text = normalizePunctuation(text)
        return text.trim()
    }

    override fun prepareForSpeech(text: String, language: SupportedLanguage): String {
        val cleaned = clean(text, language)
        return cleaned
            .replace(Regex("[•·]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun dropRepeatedHeaderFooter(text: String): String {
        val lines = text.lines()
        if (lines.size < 6) return text
        val first = lines.first().trim()
        val last = lines.last().trim()
        val body = lines.drop(1).dropLast(1)
        val dropFirst = first.isNotBlank() && body.count { it.trim() == first } >= 2
        val dropLast = last.isNotBlank() && body.count { it.trim() == last } >= 2 && last.matches(PAGE_NUMBER)
        val kept = lines.toMutableList()
        if (dropFirst) kept.removeAt(0)
        if (dropLast) kept.removeAt(kept.lastIndex)
        return kept.joinToString("\n")
    }

    private fun joinHyphenatedLineBreaks(text: String): String {
        return HYPHEN_BREAK.replace(text, "$1$2")
    }

    private fun joinBrokenLines(text: String): String {
        val lines = text.lines()
        val out = StringBuilder()
        lines.forEachIndexed { index, rawLine ->
            val line = rawLine.trimEnd()
            if (index == 0) {
                out.append(line.trimStart())
                return@forEachIndexed
            }
            val previous = out.lastOrNull()
            val currentStartsLower = line.trimStart().firstOrNull()?.let { it.isLowerCase() || isIndicLetter(it) } == true
            val previousIsLetter = previous?.let { it.isLetter() || isIndicLetter(it) } == true
            if (line.isBlank()) {
                out.append("\n\n")
            } else if (previousIsLetter && currentStartsLower && previous != '.' && previous != '।' && previous != '!') {
                out.append(' ').append(line.trimStart())
            } else {
                if (out.isNotEmpty() && out.last() != '\n') out.append('\n')
                out.append(line.trimStart())
            }
        }
        return out.toString()
    }

    private fun normalizeWhitespace(text: String): String {
        return text
            .replace(Regex("[\\t\\u00A0]+"), " ")
            .replace(Regex(" *\\n *"), "\n")
            .replace(Regex("\\n{3,}"), "\n\n")
            .replace(Regex(" {2,}"), " ")
    }

    private fun normalizePunctuation(text: String): String {
        return text
            .replace("…", "...")
            .replace(Regex("([.!?।]){4,}"), "$1$1$1")
            .replace(Regex(" ,"), ",")
            .replace(Regex(" \\."), ".")
    }

    private fun isIndicLetter(char: Char): Boolean {
        val code = char.code
        return code in 0x0900..0x097F || code in 0x0C00..0x0C7F
    }

    companion object {
        private val HYPHEN_BREAK = Regex("(\\p{L})-\\n(\\p{L})")
        private val PAGE_NUMBER = Regex("^\\d{1,4}$")
    }
}
