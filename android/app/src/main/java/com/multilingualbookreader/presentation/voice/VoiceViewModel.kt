package com.multilingualbookreader.presentation.voice

import android.app.Application
import android.media.MediaRecorder
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.multilingualbookreader.audio.SpeechPreviewPlayer
import com.multilingualbookreader.domain.engine.TextToSpeechEngine
import com.multilingualbookreader.domain.engine.VoiceCloningEngine
import com.multilingualbookreader.domain.model.SupportedLanguage
import com.multilingualbookreader.domain.model.VoiceProfile
import com.multilingualbookreader.domain.repository.SettingsRepository
import com.multilingualbookreader.domain.repository.VoiceRepository
import com.multilingualbookreader.presentation.reader.defaultStandardVoice
import com.multilingualbookreader.storage.LocalFileStore
import com.multilingualbookreader.tts.usesOnDeviceRecording
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
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
    val creating: Boolean = false,
)

@HiltViewModel
class VoiceViewModel @Inject constructor(
    application: Application,
    private val cloning: VoiceCloningEngine,
    voices: VoiceRepository,
    private val settings: SettingsRepository,
    private val preview: SpeechPreviewPlayer,
) : AndroidViewModel(application) {
    val profiles = voices.observeProfiles().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val selectedVoiceId = settings.observe()
        .map { it.selectedVoiceId }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    private val _state = MutableStateFlow(VoiceUiState())
    val state: StateFlow<VoiceUiState> = _state
    private var recorder: MediaRecorder? = null
    private var currentFile: File? = null
    private val recorded = mutableListOf<ByteArray>()
    private val draftDir = File(application.cacheDir, "voice-draft")

    init {
        draftDir.listFiles()?.sortedBy { it.name }?.forEach { file ->
            if (file.isFile && file.length() > 0) recorded += file.readBytes()
        }
        if (recorded.isNotEmpty()) {
            _state.value = _state.value.copy(samples = recorded.size)
        }
    }

    fun setName(name: String) { _state.value = _state.value.copy(name = name) }
    fun setConsent(value: Boolean) { _state.value = _state.value.copy(consent = value) }

    fun startRecording() {
        runCatching {
            val file = File(draftDir.apply { mkdirs() }, "voice-${recorded.size}.m4a")
            currentFile = file
            recorder = newRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            _state.value = _state.value.copy(recording = true, paused = false, error = null, message = null)
        }.onFailure { error ->
            runCatching { recorder?.release() }
            recorder = null
            _state.value = _state.value.copy(
                recording = false,
                error = "Could not start the microphone. ${error.message?.take(80) ?: "Try again."}",
            )
        }
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
        val stopped = runCatching { recorder?.stop(); recorder?.release() }
        recorder = null
        if (stopped.isFailure) {
            _state.value = _state.value.copy(
                recording = false,
                paused = false,
                error = "That sample could not be saved. Record it again.",
            )
            return
        }
        currentFile?.takeIf { it.exists() && it.length() > 0 }?.readBytes()?.let { recorded += it }
        persistDraft()
        _state.value = _state.value.copy(recording = false, paused = false, samples = recorded.size, error = null)
    }

    fun retakeLast() {
        if (recorded.isNotEmpty()) recorded.removeAt(recorded.lastIndex)
        persistDraft()
        _state.value = _state.value.copy(samples = recorded.size)
    }

    fun playLastSample() {
        val bytes = recorded.lastOrNull()
        if (bytes == null) {
            _state.value = _state.value.copy(error = "Record a sample first.")
            return
        }
        runCatching { preview.play(bytes, "audio/mp4") }
            .onSuccess { _state.value = _state.value.copy(message = "Playing your last sample.", error = null) }
            .onFailure { _state.value = _state.value.copy(error = "Could not play that sample.") }
    }

    fun create() {
        viewModelScope.launch { createNow() }
    }

    internal fun addSampleForTest(bytes: ByteArray = byteArrayOf(1, 2, 3)) {
        recorded += bytes
        _state.value = _state.value.copy(samples = recorded.size)
    }

    internal suspend fun createNow() {
        val current = _state.value
        when {
            current.creating -> return
            !current.consent -> {
                _state.value = current.copy(error = "Tick the box to confirm you are allowed to use this voice.")
                return
            }
            recorded.isEmpty() -> {
                _state.value = current.copy(error = "Record at least one sample, then tap Create voice.")
                return
            }
        }
        _state.value = current.copy(creating = true, error = null, message = "Saving your voice…")
        runCatching {
            cloning.createVoice(current.name, recorded.toList(), true)
        }.onSuccess { profile ->
            recorded.clear()
            persistDraft()
            settings.update { it.copy(selectedVoiceId = profile.id) }
            _state.value = VoiceUiState(
                message = "Voice saved as ${profile.name}. Open Compare voices to hear English, Hindi, and Telugu.",
            )
        }.onFailure { error ->
            _state.value = current.copy(
                creating = false,
                error = (error as? com.multilingualbookreader.domain.AppError)?.userMessage
                    ?: error.message?.takeIf { it.isNotBlank() }
                    ?: "Your voice profile could not be created.",
            )
        }
    }

    fun delete(id: String) {
        viewModelScope.launch { cloning.deleteVoice(id) }
    }

    fun select(profile: VoiceProfile) {
        viewModelScope.launch { selectNow(profile) }
    }

    internal suspend fun selectNow(profile: VoiceProfile) {
        settings.update { it.copy(selectedVoiceId = profile.id) }
        _state.value = _state.value.copy(
            message = "Using ${profile.name}. Compare voices plays your recording. Books use this phone's speaker until a clone is ready.",
            error = null,
        )
    }

    private fun persistDraft() {
        draftDir.mkdirs()
        draftDir.listFiles()?.forEach { it.delete() }
        recorded.forEachIndexed { index, bytes ->
            File(draftDir, "sample-${index.toString().padStart(2, '0')}.m4a").writeBytes(bytes)
        }
    }

    @Suppress("DEPRECATION")
    private fun newRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(getApplication())
        } else {
            MediaRecorder()
        }
}

data class VoiceTestUiState(
    val message: String = "Enter text and play. A voice you recorded plays your saved sample. Standard voice uses this phone's speaker unless a reading server is set.",
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
    private val files: LocalFileStore,
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
        if (profile.usesOnDeviceRecording()) {
            val sample = files.latestVoiceSample(profile.id)
            if (sample != null) {
                preview.play(sample.first, sample.second) {
                    _ui.value = _ui.value.copy(
                        playingLanguage = null,
                        message = "Finished your ${profile.name} recording.",
                    )
                }
                _ui.value = _ui.value.copy(
                    playingLanguage = language,
                    message = "Playing your ${profile.name} recording. This is the sample you saved, not the typed sentence. Books still use this phone's speaker until a clone is ready.",
                )
                return
            }
        }
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
            return selectedId?.let { id -> profiles.firstOrNull { it.id == id } }
                ?: defaultStandardVoice()
        }
    }
}
