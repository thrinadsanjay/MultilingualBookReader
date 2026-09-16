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
    /** Telugu has no on-device model, so it is the one script that always needs the server. */
    fun shouldTryBackend(
        localText: String?,
        hintLanguage: SupportedLanguage?,
        online: Boolean,
    ): Boolean = when {
        !online -> false
        localText.isNullOrBlank() -> true
        hintLanguage == SupportedLanguage.TELUGU -> true
        containsTelugu(localText) -> true
        looksUnreliable(localText) -> true
        else -> false
    }

    fun containsTelugu(text: String): Boolean = text.any { it.code in TELUGU_RANGE }

    /**
     * ML Kit will still emit *something* for a sideways Telugu page — punctuation, a few Latin
     * letters, maybe a stray Telugu glyph. That is not a successful read.
     */
    fun looksUnreliable(text: String): Boolean {
        val letters = text.count { it.isLetter() }
        val symbols = text.count { !it.isLetter() && !it.isWhitespace() && !it.isDigit() }
        if (letters < 8) return true
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
    ): OcrSource? = when {
        !backendText.isNullOrBlank() -> OcrSource.BACKEND
        !localText.isNullOrBlank() -> OcrSource.LOCAL
        localText != null && !backendAttempted -> OcrSource.LOCAL
        else -> null
    }
}
