package com.multilingualbookreader.ocr

import com.multilingualbookreader.domain.model.SupportedLanguage

enum class OcrSource { LOCAL, BACKEND }

/**
 * Decides when on-device recognition needs help from the server.
 *
 * Confidence deliberately plays no part. ML Kit's text recognizer does not report a confidence for
 * on-device results, so every page scored zero and the app sent perfectly good text to the backend,
 * losing it when that backend was unreachable.
 */
object OcrFallbackPolicy {
    /** Tries the server when the on-device read still looks like garbage or empty. */
    fun shouldTryBackend(
        localText: String?,
        hintLanguage: SupportedLanguage?,
        online: Boolean,
    ): Boolean = when {
        !online -> false
        localText.isNullOrBlank() -> true
        containsTelugu(localText) && !looksUnreliable(localText) -> false
        hintLanguage == SupportedLanguage.TELUGU -> true
        containsTelugu(localText) -> true
        looksUnreliable(localText) -> true
        else -> false
    }

    fun containsTelugu(text: String): Boolean = text.any { it.code in TELUGU_RANGE }

    /**
     * Picks the read that actually looks like a page of writing. Telugu from the on-device
     * Tesseract model beats ML Kit garbage even when both returned *something*.
     */
    fun better(first: String?, second: String?): String? {
        val candidates = listOfNotNull(first, second).filter { it.isNotBlank() }
        if (candidates.isEmpty()) return null
        return candidates.maxWith(
            compareBy<String> { containsTelugu(it) && !looksUnreliable(it) }
                .thenBy { !looksUnreliable(it) }
                .thenBy { containsTelugu(it) }
                .thenBy { it.length },
        )
    }

    /**
     * ML Kit will still emit *something* for a sideways Telugu page — punctuation, a few Latin
     * letters, maybe a stray Telugu glyph. That is not a successful read.
     */
    fun looksUnreliable(text: String): Boolean {
        val telugu = text.count { it.code in TELUGU_RANGE }
        val letters = text.count { it.isLetter() || it.code in TELUGU_RANGE }
        val symbols = text.count { !it.isLetter() && !it.isWhitespace() && !it.isDigit() && it.code !in TELUGU_RANGE }
        val words = text.split(Regex("[\\s\\d\\p{Punct}]+")).count { token ->
            token.length >= 3 && token.any { it.isLetter() || it.code in TELUGU_RANGE }
        }
        if (telugu >= 12 && symbols * 2 < telugu) return false
        if (letters < 8) return true
        if (words < 4) return true
        return symbols * 2 >= letters
    }

    private val TELUGU_RANGE = 0x0C00..0x0C7F

    /**
     * Picks the attempt that actually produced text. A failed round trip must never discard text
     * the device already read.
     *
     * @param backendAttempted whether the server was asked. When it was asked and neither side
     * produced text, the page is treated as unreadable rather than silently empty.
     */
    fun choose(
        localText: String?,
        backendText: String?,
        backendAttempted: Boolean,
    ): OcrSource? {
        val winner = better(localText, backendText)
        return when {
            winner != null && winner == backendText -> OcrSource.BACKEND
            winner != null -> OcrSource.LOCAL
            localText != null && !backendAttempted -> OcrSource.LOCAL
            else -> null
        }
    }
}
