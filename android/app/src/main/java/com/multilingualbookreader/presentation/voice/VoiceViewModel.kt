package com.multilingualbookreader.presentation.voice

import android.app.Application
import android.media.MediaRecorder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.multilingualbookreader.audio.SpeechPreviewPlayer
import com.multilingualbookreader.domain.engine.TextToSpeechEngine
import com.multilingualbookreader.domain.engine.VoiceCloningEngine
import com.multilingualbookreader.domain.model.SupportedLanguage
import com.multilingualbookreader.domain.model.VoiceProfile
import com.multilingualbookreader.domain.model.VoiceStatus
import com.multilingualbookreader.domain.repository.SettingsRepository
import com.multilingualbookreader.domain.repository.VoiceRepository
import com.multilingualbookreader.presentation.reader.defaultStandardVoice
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class VoiceUiState(
    val consent: Boolean = false,
    val recording: Boolean = false,
    val paused: Boolean = false,
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
        _state.value = _state.value.copy(recording = true, paused = false, error = null)
    }

    fun pauseRecording() {
        runCatching { recorder?.pause() }
        _state.value = _state.value.copy(paused = true)
    }

    fun resumeRecording() {
        runCatching { recorder?.resume() }
        _state.value = _state.value.copy(paused = false)
    }

    fun stopRecording() {
        runCatching { recorder?.stop(); recorder?.release() }
        recorder = null
        currentFile?.takeIf { it.exists() }?.readBytes()?.let { recorded += it }
        _state.value = _state.value.copy(recording = false, paused = false, samples = recorded.size)
    }

    fun retakeLast() {
        if (recorded.isNotEmpty()) recorded.removeAt(recorded.lastIndex)
        _state.value = _state.value.copy(samples = recorded.size)
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

data class VoiceTestUiState(
    val message: String = "Enter text and play. Compare the standard voice with a voice you recorded.",
    val selectedVoiceId: String = defaultStandardVoice().id,
    val playingLanguage: SupportedLanguage? = null,
)

@HiltViewModel
class VoiceQualityTestViewModel @Inject constructor(
    application: Application,
    private val tts: TextToSpeechEngine,
    voices: VoiceRepository,
    private val settings: SettingsRepository,
    private val preview: SpeechPreviewPlayer,
) : AndroidViewModel(application) {
    val profiles = voices.observeProfiles().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val _ui = MutableStateFlow(VoiceTestUiState())
    val ui: StateFlow<VoiceTestUiState> = _ui
    val english = MutableStateFlow("Welcome to my book reader.")
    val hindi = MutableStateFlow("यह मेरी किताब है।")
    val telugu = MutableStateFlow("ఇది నా పుస్తకం.")
    private var playJob: Job? = null

    init {
        viewModelScope.launch {
            val selectedId = settings.get().selectedVoiceId
            if (selectedId != null) _ui.value = _ui.value.copy(selectedVoiceId = selectedId)
        }
    }

    fun selectVoice(id: String) {
        _ui.value = _ui.value.copy(selectedVoiceId = id)
    }

    fun play(text: String, language: SupportedLanguage, voice: VoiceProfile? = null) {
        playJob?.cancel()
        preview.stop()
        playJob = viewModelScope.launch { playNow(text, language, voice) }
    }

    internal suspend fun playNow(text: String, language: SupportedLanguage, voice: VoiceProfile? = null) {
        val spoken = text.trim()
        if (spoken.isEmpty()) {
            _ui.value = _ui.value.copy(playingLanguage = null, message = "Type something to hear, then play.")
            return
        }
        val profile = voice ?: resolveVoice(profiles.value, _ui.value.selectedVoiceId)
        _ui.value = _ui.value.copy(
            playingLanguage = language,
            message = "Generating ${language.displayName} with ${profile.name}…",
        )
        runCatching {
            val result = withContext(Dispatchers.IO) {
                tts.synthesize(spoken, language.bcp47, profile, 1.0f)
            }
            if (result.bytes.isEmpty()) error("No speech was generated.")
            preview.play(result.bytes, result.mimeType) {
                _ui.value = _ui.value.copy(
                    playingLanguage = null,
                    message = "Finished ${language.displayName} with ${profile.name}.",
                )
            }
            _ui.value = _ui.value.copy(
                playingLanguage = language,
                message = "Playing ${language.displayName} with ${profile.name}. Listen and decide if it sounds natural.",
            )
        }.onFailure { error ->
            preview.stop()
            _ui.value = _ui.value.copy(
                playingLanguage = null,
                message = error.message?.takeIf { it.isNotBlank() }
                    ?: "Could not play speech. If you are offline, only the device voice works.",
            )
        }
    }

    override fun onCleared() {
        preview.stop()
        super.onCleared()
    }

    companion object {
        fun resolveVoice(profiles: List<VoiceProfile>, selectedId: String?): VoiceProfile {
            val selected = selectedId?.let { id -> profiles.firstOrNull { it.id == id } }
            val usable = selected ?: profiles.firstOrNull { it.status == VoiceStatus.READY }
            return when {
                usable == null -> defaultStandardVoice()
                usable.status != VoiceStatus.READY || usable.provider == "pending-upload" -> defaultStandardVoice()
                else -> usable
            }
        }
    }
}
