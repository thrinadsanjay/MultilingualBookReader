package com.multilingualbookreader.ocr

import com.google.common.truth.Truth.assertThat
import com.multilingualbookreader.domain.model.OcrResult
import com.multilingualbookreader.domain.model.SupportedLanguage
import com.multilingualbookreader.language.ScriptLanguageDetector
import com.multilingualbookreader.text.DefaultTextProcessor
import org.junit.Test

class OcrCleanupPipelineTest {
    private val processor = DefaultTextProcessor()
    private val detector = ScriptLanguageDetector()

    @Test
    fun cleansEnglishOcrBeforeSpeech() {
        val raw = "Once up-\non a time.\n\n\nHeader\nHeader"
        val cleaned = processor.prepareForSpeech(processor.clean(raw), SupportedLanguage.ENGLISH)
        assertThat(cleaned).contains("upon")
        assertThat(detector.detect(cleaned)).isEqualTo(SupportedLanguage.ENGLISH)
    }

    @Test
    fun keepsTeluguAndHindiIntact() {
        val telugu = "ఈ రోజు మనం ఒక మంచి కథ చదువుదాం."
        val hindi = "आज हम एक अच्छी कहानी पढ़ेंगे।"
        assertThat(processor.clean(telugu, SupportedLanguage.TELUGU)).isEqualTo(telugu)
        assertThat(processor.clean(hindi, SupportedLanguage.HINDI)).isEqualTo(hindi)
    }

    @Test
    fun emptyOcrResultHasUnknownLanguage() {
        val result = OcrResult("", SupportedLanguage.UNKNOWN, 0f, emptyList(), "test", true)
        assertThat(result.text).isEmpty()
    }
}
