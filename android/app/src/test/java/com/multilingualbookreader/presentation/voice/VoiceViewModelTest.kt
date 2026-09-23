package com.multilingualbookreader.presentation.voice

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.multilingualbookreader.audio.SpeechPreviewPlayer
import com.multilingualbookreader.domain.engine.VoiceCloningEngine
import com.multilingualbookreader.domain.model.AppSettings
import com.multilingualbookreader.domain.model.VoiceProfile
import com.multilingualbookreader.domain.model.VoiceStatus
import com.multilingualbookreader.domain.repository.SettingsRepository
import com.multilingualbookreader.domain.repository.VoiceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class VoiceViewModelTest {
    @Test
    fun createAsksForConsentAndASample() = runBlocking {
        val vm = viewModel()
        vm.createNow()
        assertThat(vm.state.value.error).contains("consent")
        vm.setConsent(true)
        vm.createNow()
        assertThat(vm.state.value.error).contains("sample")
    }

    @Test
    fun createSavesAVoiceAfterOneSample() = runBlocking {
        val cloning = FakeCloning()
        val settings = FakeSettings()
        val vm = viewModel(cloning = cloning, settings = settings)
        vm.setConsent(true)
        vm.addSampleForTest()
        vm.createNow()
        assertThat(cloning.created).isTrue()
        assertThat(vm.state.value.message).contains("Voice saved")
        assertThat(settings.value.selectedVoiceId).isEqualTo("saved")
        assertThat(vm.state.value.samples).isEqualTo(0)
    }

    @Test
    fun localDraftKeepsTheVoiceOnThePhoneWhenTheServerIsMissing() {
        val draft = com.multilingualbookreader.voice.BackendVoiceCloningEngine.localDraft("id", "Amma", 1)
        assertThat(draft.status).isEqualTo(VoiceStatus.DRAFT)
        assertThat(draft.qualityNote).contains("Saved on this phone")
    }

    private fun viewModel(
        cloning: VoiceCloningEngine = FakeCloning(),
        settings: FakeSettings = FakeSettings(),
    ) = VoiceViewModel(
        ApplicationProvider.getApplicationContext(),
        cloning,
        FakeVoices(),
        settings,
        FakePreview(),
    )

    private class FakeCloning : VoiceCloningEngine {
        var created = false
        override val name = "fake"
        override suspend fun createVoice(
            name: String,
            sampleWavFiles: List<ByteArray>,
            consentConfirmed: Boolean,
        ): VoiceProfile {
            created = true
            return VoiceProfile(
                id = "saved",
                name = name,
                provider = "pending-upload",
                providerVoiceId = null,
                supportedLanguages = emptyList(),
                status = VoiceStatus.DRAFT,
                isCloned = true,
                createdAt = 1,
                updatedAt = 1,
            )
        }
        override suspend fun getVoice(id: String) = error("unused")
        override suspend fun listVoices() = emptyList<VoiceProfile>()
        override suspend fun deleteVoice(id: String) = Unit
    }

    private class FakeVoices : VoiceRepository {
        private val items = MutableStateFlow<List<VoiceProfile>>(emptyList())
        override fun observeProfiles(): Flow<List<VoiceProfile>> = items
        override suspend fun get(id: String) = items.value.firstOrNull { it.id == id }
        override suspend fun upsert(profile: VoiceProfile) = Unit
        override suspend fun delete(id: String) = Unit
    }

    private class FakeSettings : SettingsRepository {
        var value = AppSettings()
        override fun observe() = MutableStateFlow(value)
        override suspend fun get() = value
        override suspend fun update(transform: (AppSettings) -> AppSettings) {
            value = transform(value)
        }
    }

    private class FakePreview : SpeechPreviewPlayer {
        override fun play(bytes: ByteArray, mimeType: String, onFinished: () -> Unit) = Unit
        override fun stop() = Unit
    }
}
