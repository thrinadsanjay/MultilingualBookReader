package com.multilingualbookreader.presentation.voice

import android.app.Application
import android.media.MediaRecorder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.multilingualbookreader.domain.engine.TextToSpeechEngine
import com.multilingualbookreader.domain.engine.VoiceCloningEngine
import com.multilingualbookreader.domain.model.SupportedLanguage
import com.multilingualbookreader.domain.model.VoiceProfile
import com.multilingualbookreader.domain.repository.SettingsRepository
import com.multilingualbookreader.domain.repository.VoiceRepository
import com.multilingualbookreader.presentation.reader.defaultStandardVoice
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class VoiceUiState(
    val consent: Boolean = false,
    val recording: Boolean = false,
    val samples: Int = 0,
    val name: String = "My voice",
    val message: String? = null,
    val error: String? = null,
)

@HiltViewModel
class VoiceViewModel @Inject constructor(
    application: Application,
    private val cloning: VoiceCloningEngine,
    voices: VoiceRepository,
    private val settings: SettingsRepository,
) : AndroidViewModel(application) {
    val profiles = voices.observeProfiles().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val _state = MutableStateFlow(VoiceUiState())
    val state: StateFlow<VoiceUiState> = _state
    private var recorder: MediaRecorder? = null
    private var currentFile: File? = null
    private val recorded = mutableListOf<ByteArray>()

    fun setName(name: String) { _state.value = _state.value.copy(name = name) }
    fun setConsent(value: Boolean) { _state.value = _state.value.copy(consent = value) }

    fun startRecording() {
        val file = File(getApplication<Application>().cacheDir, "voice-${recorded.size}.m4a")
        currentFile = file
        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128000)
            setAudioSamplingRate(44100)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
        _state.value = _state.value.copy(recording = true, error = null)
    }

    fun stopRecording() {
        runCatching { recorder?.stop(); recorder?.release() }
        recorder = null
        currentFile?.takeIf { it.exists() }?.readBytes()?.let { recorded += it }
        _state.value = _state.value.copy(recording = false, samples = recorded.size)
    }

    fun create() {
        viewModelScope.launch {
            if (!_state.value.consent) {
                _state.value = _state.value.copy(error = "Please confirm you are allowed to use this voice.")
                return@launch
            }
            runCatching {
                cloning.createVoice(_state.value.name, recorded.toList(), true)
            }.onSuccess {
                recorded.clear()
                _state.value = VoiceUiState(message = "Voice saved. Test Telugu, Hindi, and English before relying on it.")
            }.onFailure {
                _state.value = _state.value.copy(error = "Your voice profile could not be created.")
            }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch { cloning.deleteVoice(id) }
    }

    fun select(profile: VoiceProfile) {
        viewModelScope.launch { settings.update { it.copy(selectedVoiceId = profile.id) } }
    }
}

@HiltViewModel
class VoiceQualityTestViewModel @Inject constructor(
    application: Application,
    private val tts: TextToSpeechEngine,
    voices: VoiceRepository,
) : AndroidViewModel(application) {
    val profiles = voices.observeProfiles().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val _message = MutableStateFlow("Enter text and play. Compare standard and custom voices.")
    val message: StateFlow<String> = _message
    val english = MutableStateFlow("Welcome to my book reader.")
    val hindi = MutableStateFlow("यह मेरी किताब है।")
    val telugu = MutableStateFlow("ఇది నా పుస్తకం.")

    fun play(text: String, language: SupportedLanguage, voice: VoiceProfile?) {
        viewModelScope.launch {
            val profile = voice ?: defaultStandardVoice()
            runCatching {
                tts.synthesize(text, language.bcp47, profile, 1.0f)
                _message.value = "Generated with ${profile.name} (${profile.provider}). Listen and decide if ${language.displayName} sounds natural. Custom cloned voices are not claimed to match Telugu or Hindi unless the provider lists them as supported."
            }.onFailure {
                _message.value = "Could not generate speech. If you are offline, only cached or device voices work."
            }
        }
    }
}
