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
        else -> hintLanguage == SupportedLanguage.TELUGU
    }

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
