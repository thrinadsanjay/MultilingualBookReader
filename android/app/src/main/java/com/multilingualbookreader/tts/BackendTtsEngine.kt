package com.multilingualbookreader.tts

import com.multilingualbookreader.audio.AudioCacheKey
import com.multilingualbookreader.domain.AppError
import com.multilingualbookreader.domain.engine.TextToSpeechEngine
import com.multilingualbookreader.domain.model.AudioResult
import com.multilingualbookreader.domain.model.VoiceProfile
import com.multilingualbookreader.network.BookReaderApi
import com.multilingualbookreader.network.ConnectivityObserver
import com.multilingualbookreader.network.TtsRequest
import com.multilingualbookreader.storage.LocalFileStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackendTtsEngine @Inject constructor(
    private val api: BookReaderApi,
    private val connectivity: ConnectivityObserver,
    private val files: LocalFileStore,
) : TextToSpeechEngine {
    override val name: String = "backend-tts"
    override val supportsOffline: Boolean = false

    override suspend fun synthesize(
        text: String,
        language: String,
        voice: VoiceProfile,
        speed: Float,
    ): AudioResult {
        val cacheKey = AudioCacheKey.of(text, language, voice.providerVoiceId ?: voice.id, speed, voice.provider)
        val cached = files.audioFile(cacheKey, "mp3")
        val wavCached = files.audioFile(cacheKey, "wav")
        when {
            cached.exists() && cached.length() > 0 -> {
                return AudioResult(cached.readBytes(), "audio/mpeg", 0, cacheKey, true, voice.provider)
            }
            wavCached.exists() && wavCached.length() > 0 -> {
                return AudioResult(wavCached.readBytes(), "audio/wav", 0, cacheKey, true, voice.provider)
            }
        }
        if (!connectivity.isOnline) throw AppError.Offline("tts")
        val body = api.tts(
            TtsRequest(
                text = text,
                language = language,
                voiceId = voice.providerVoiceId ?: voice.id,
                speed = speed,
            ),
        )
        val bytes = body.bytes()
        val mime = body.contentType()?.toString() ?: "audio/mpeg"
        val extension = if (mime.contains("wav")) "wav" else "mp3"
        val path = files.saveAudio(cacheKey, bytes, extension)
        return AudioResult(bytes, mime, 0, cacheKey, false, voice.provider).also {
            check(FileExists(path))
        }
    }

    private fun FileExists(path: String) = java.io.File(path).exists()
}
