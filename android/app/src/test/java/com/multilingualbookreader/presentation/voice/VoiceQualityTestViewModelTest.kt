package com.multilingualbookreader.presentation.voice

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.multilingualbookreader.audio.SpeechPreviewPlayer
import com.multilingualbookreader.domain.engine.TextToSpeechEngine
import com.multilingualbookreader.domain.model.AppSettings
import com.multilingualbookreader.domain.model.AudioResult
import com.multilingualbookreader.domain.model.SupportedLanguage
import com.multilingualbookreader.domain.model.VoiceProfile
import com.multilingualbookreader.domain.model.VoiceStatus
import com.multilingualbookreader.domain.repository.SettingsRepository
import com.multilingualbookreader.domain.repository.VoiceRepository
import com.multilingualbookreader.presentation.reader.defaultStandardVoice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class VoiceQualityTestViewModelTest {
    @Test
    fun playSendsSynthesizedBytesToTheSpeaker() = runBlocking {
        val tts = FakeTts()
        val preview = FakePreview()
        val vm = VoiceQualityTestViewModel(
            ApplicationProvider.getApplicationContext(),
            tts,
            FakeVoices(),
            FakeSettings(),
            preview,
        )
        vm.play("Welcome to my book reader.", SupportedLanguage.ENGLISH, defaultStandardVoice())
        kotlinx.coroutines.delay(80)
        assertThat(preview.played?.decodeToString()).isEqualTo("speech-bytes")
        assertThat(preview.mime).isEqualTo("audio/wav")
        assertThat(vm.ui.value.message).contains("Playing English")
        assertThat(vm.ui.value.playingLanguage).isEqualTo(SupportedLanguage.ENGLISH)
    }

    @Test
    fun resolveVoiceFallsBackWhenTheCloneIsStillUploading() {
        val draft = VoiceProfile(
            id = "mine",
            name = "My voice",
            provider = "pending-upload",
            providerVoiceId = null,
            supportedLanguages = emptyList(),
            status = VoiceStatus.DRAFT,
            isCloned = true,
            createdAt = 1,
            updatedAt = 1,
        )
        val voice = VoiceQualityTestViewModel.resolveVoice(listOf(draft), "mine")
        assertThat(voice.id).isEqualTo(defaultStandardVoice().id)
    }

    private class FakeTts : TextToSpeechEngine {
        override val name = "fake-tts"
        override val supportsOffline = true
        override suspend fun synthesize(
            text: String,
            language: String,
            voice: VoiceProfile,
            speed: Float,
        ) = AudioResult("speech-bytes".toByteArray(), "audio/wav", 10, "key", false, name)
    }

    private class FakePreview : SpeechPreviewPlayer {
        var played: ByteArray? = null
        var mime: String? = null
        override fun play(bytes: ByteArray, mimeType: String, onFinished: () -> Unit) {
            played = bytes
            mime = mimeType
        }
        override fun stop() = Unit
    }

    private class FakeVoices : VoiceRepository {
        private val items = MutableStateFlow<List<VoiceProfile>>(emptyList())
        override fun observeProfiles(): Flow<List<VoiceProfile>> = items
        override suspend fun get(id: String) = items.value.firstOrNull { it.id == id }
        override suspend fun upsert(profile: VoiceProfile) = Unit
        override suspend fun delete(id: String) = Unit
    }

    private class FakeSettings : SettingsRepository {
        private val value = MutableStateFlow(AppSettings())
        override fun observe() = value
        override suspend fun get() = value.value
        override suspend fun update(transform: (AppSettings) -> AppSettings) {
            value.value = transform(value.value)
        }
    }
}
