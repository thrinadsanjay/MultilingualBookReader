package com.multilingualbookreader.text

import com.google.common.truth.Truth.assertThat
import com.multilingualbookreader.domain.model.SupportedLanguage
import com.multilingualbookreader.language.ScriptLanguageDetector
import org.junit.Test

class SentenceSegmenterTest {
    private val segmenter = SentenceSegmenter(ScriptLanguageDetector())

    @Test
    fun splitsEnglishSentences() {
        val segments = segmenter.segment("Hello there. How are you?")
        assertThat(segments).hasSize(2)
        assertThat(segments[0].text).isEqualTo("Hello there.")
        assertThat(segments[1].text).isEqualTo("How are you?")
        assertThat(segments[0].language).isEqualTo(SupportedLanguage.ENGLISH)
    }

    @Test
    fun splitsHindiDanda() {
        val segments = segmenter.segment("यह मेरी किताब है। यह अच्छी है।")
        assertThat(segments.size).isAtLeast(2)
        assertThat(segments.first().language).isEqualTo(SupportedLanguage.HINDI)
    }

    @Test
    fun keepsTeluguSentence() {
        val text = "ఇది నా పుస్తకం."
        val segments = segmenter.segment(text)
        assertThat(segments).hasSize(1)
        assertThat(segments.first().language).isEqualTo(SupportedLanguage.TELUGU)
    }
}
