package com.multilingualbookreader.language

import com.google.common.truth.Truth.assertThat
import com.multilingualbookreader.domain.model.SupportedLanguage
import org.junit.Test

class ScriptLanguageDetectorTest {
    private val detector = ScriptLanguageDetector()

    @Test
    fun detectsEnglish() {
        assertThat(detector.detect("Today we will read a good story.")).isEqualTo(SupportedLanguage.ENGLISH)
    }

    @Test
    fun detectsHindi() {
        assertThat(detector.detect("आज हम एक अच्छी कहानी पढ़ेंगे।")).isEqualTo(SupportedLanguage.HINDI)
    }

    @Test
    fun detectsTelugu() {
        assertThat(detector.detect("ఈ రోజు మనం ఒక మంచి కథ చదువుదాం.")).isEqualTo(SupportedLanguage.TELUGU)
    }

    @Test
    fun detectsMixedLanguage() {
        val result = detector.detect("రాముడు went to the market.")
        assertThat(result).isEqualTo(SupportedLanguage.MIXED)
    }

    @Test
    fun splitsMixedSpans() {
        val spans = detector.detectSpans("రాముడు went to the market.")
        val languages = spans.map { it.language }
        assertThat(languages).contains(SupportedLanguage.TELUGU)
        assertThat(languages).contains(SupportedLanguage.ENGLISH)
    }
}
