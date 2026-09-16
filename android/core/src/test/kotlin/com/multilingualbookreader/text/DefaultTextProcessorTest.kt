package com.multilingualbookreader.text

import com.google.common.truth.Truth.assertThat
import com.multilingualbookreader.domain.model.SupportedLanguage
import org.junit.Test

class DefaultTextProcessorTest {
    private val processor = DefaultTextProcessor()

    @Test
    fun joinsHyphenatedEnglishLineBreaks() {
        val raw = "informa-\ntion"
        assertThat(processor.clean(raw, SupportedLanguage.ENGLISH)).isEqualTo("information")
    }

    @Test
    fun preservesTeluguWords() {
        val raw = "రాముడు ఒక రోజు\nగ్రామానికి వెళ్లాడు."
        val cleaned = processor.clean(raw, SupportedLanguage.TELUGU)
        assertThat(cleaned).contains("రాముడు")
        assertThat(cleaned).contains("గ్రామానికి")
    }

    @Test
    fun collapsesRepeatedBlankLines() {
        val raw = "Hello\n\n\n\nworld"
        assertThat(processor.clean(raw, SupportedLanguage.ENGLISH)).isEqualTo("Hello\n\nworld")
    }

    @Test
    fun doesNotRewriteIndicTextAggressively() {
        val raw = "आज हम एक अच्छी कहानी पढ़ेंगे।"
        assertThat(processor.clean(raw, SupportedLanguage.HINDI)).isEqualTo(raw)
    }
}
