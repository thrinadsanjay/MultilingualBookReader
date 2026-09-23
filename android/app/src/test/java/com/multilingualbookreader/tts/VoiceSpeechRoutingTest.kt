package com.multilingualbookreader.tts

import com.google.common.truth.Truth.assertThat
import com.multilingualbookreader.domain.model.VoiceProfile
import com.multilingualbookreader.domain.model.VoiceStatus
import com.multilingualbookreader.presentation.reader.defaultStandardVoice
import org.junit.Test

class VoiceSpeechRoutingTest {
    @Test
    fun draftRecordingStaysOnThePhone() {
        val draft = VoiceProfile(
            id = "sanjay",
            name = "Sanjay",
            provider = "pending-upload",
            providerVoiceId = null,
            supportedLanguages = emptyList(),
            status = VoiceStatus.DRAFT,
            isCloned = true,
            createdAt = 1,
            updatedAt = 1,
        )
        assertThat(draft.usesOnDeviceRecording()).isTrue()
        assertThat(prefersDeviceSpeech(draft, online = true, serverConfigured = true)).isTrue()
        assertThat(prefersDeviceSpeech(draft, online = true, serverConfigured = false)).isTrue()
    }

    @Test
    fun standardVoiceSkipsTheServerWhenNoneIsSet() {
        val standard = defaultStandardVoice()
        assertThat(standard.usesOnDeviceRecording()).isFalse()
        assertThat(prefersDeviceSpeech(standard, online = true, serverConfigured = false)).isTrue()
        assertThat(prefersDeviceSpeech(standard, online = true, serverConfigured = true)).isFalse()
    }
}
