package com.multilingualbookreader.ocr

import com.google.common.truth.Truth.assertThat
import com.multilingualbookreader.domain.model.SupportedLanguage
import org.junit.Test

class OcrFallbackPolicyTest {
    @Test
    fun keepsOnDeviceTextInsteadOfCallingTheServer() {
        val tryBackend = OcrFallbackPolicy.shouldTryBackend(
            localText = "The quick brown fox jumps over the lazy dog.",
            hintLanguage = SupportedLanguage.ENGLISH,
            online = true,
        )
        assertThat(tryBackend).isFalse()
    }

    @Test
    fun asksTheServerWhenTheDeviceReadNothing() {
        assertThat(OcrFallbackPolicy.shouldTryBackend("", SupportedLanguage.ENGLISH, online = true)).isTrue()
        assertThat(OcrFallbackPolicy.shouldTryBackend(null, null, online = true)).isTrue()
    }

    @Test
    fun teluguAlwaysNeedsTheServerBecauseThereIsNoOnDeviceModel() {
        val tryBackend = OcrFallbackPolicy.shouldTryBackend(
            localText = "garbled latin guess",
            hintLanguage = SupportedLanguage.TELUGU,
            online = true,
        )
        assertThat(tryBackend).isTrue()
    }

    @Test
    fun offlineNeverReachesForTheServer() {
        assertThat(OcrFallbackPolicy.shouldTryBackend(null, SupportedLanguage.TELUGU, online = false)).isFalse()
    }

    @Test
    fun anUnreachableServerDoesNotThrowAwayLocalText() {
        val source = OcrFallbackPolicy.choose(
            localText = "Chapter one",
            backendText = null,
            backendAttempted = true,
        )
        assertThat(source).isEqualTo(OcrSource.LOCAL)
    }

    @Test
    fun serverTextWinsWhenItHasSomething() {
        assertThat(OcrFallbackPolicy.choose("", "తెలుగు వచనం", backendAttempted = true)).isEqualTo(OcrSource.BACKEND)
    }

    @Test
    fun anEmptyPageReadOfflineIsStillASuccess() {
        assertThat(OcrFallbackPolicy.choose("", null, backendAttempted = false)).isEqualTo(OcrSource.LOCAL)
    }

    @Test
    fun nothingAnywhereIsAFailure() {
        assertThat(OcrFallbackPolicy.choose("", null, backendAttempted = true)).isNull()
        assertThat(OcrFallbackPolicy.choose(null, null, backendAttempted = true)).isNull()
    }
}
