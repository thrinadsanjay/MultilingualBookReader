package com.multilingualbookreader.audio

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AudioCacheKeyTest {
    @Test
    fun sameInputsProduceSameKey() {
        val a = AudioCacheKey.of("Hello", "en", "voice-1", 1.0f, "google")
        val b = AudioCacheKey.of("Hello", "en", "voice-1", 1.0f, "google")
        assertThat(a).isEqualTo(b)
        assertThat(a).hasLength(64)
    }

    @Test
    fun differentVoiceProducesDifferentKey() {
        val a = AudioCacheKey.of("Hello", "en", "voice-1", 1.0f)
        val b = AudioCacheKey.of("Hello", "en", "voice-2", 1.0f)
        assertThat(a).isNotEqualTo(b)
    }

    @Test
    fun speedIsPartOfKey() {
        val a = AudioCacheKey.of("Hello", "en", "voice-1", 1.0f)
        val b = AudioCacheKey.of("Hello", "en", "voice-1", 1.25f)
        assertThat(a).isNotEqualTo(b)
    }

    @Test
    fun teluguTextIsHashedWithoutLoggingContent() {
        val key = AudioCacheKey.of("ఇది నా పుస్తకం.", "te", "voice-1", 1.0f)
        assertThat(key).doesNotContain("పుస్తకం")
    }
}
