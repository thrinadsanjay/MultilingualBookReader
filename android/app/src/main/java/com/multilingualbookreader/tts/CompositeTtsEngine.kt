package com.multilingualbookreader.tts

import com.multilingualbookreader.common.AppLog
import com.multilingualbookreader.domain.engine.TextToSpeechEngine
import com.multilingualbookreader.domain.model.AudioResult
import com.multilingualbookreader.domain.model.VoiceProfile
import com.multilingualbookreader.network.ConnectivityObserver
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CompositeTtsEngine @Inject constructor(
    private val backend: BackendTtsEngine,
    private val android: AndroidTtsEngine,
    private val connectivity: ConnectivityObserver,
) : TextToSpeechEngine {
    override val name: String = "composite-tts"
    override val supportsOffline: Boolean = true

    override suspend fun synthesize(
        text: String,
        language: String,
        voice: VoiceProfile,
        speed: Float,
    ): AudioResult {
        val preferDevice = voice.provider == android.name || !connectivity.isOnline
        val result = if (preferDevice) {
            runCatching { android.synthesize(text, language, voice, speed) }
                .getOrElse {
                    if (connectivity.isOnline) backend.synthesize(text, language, voice, speed) else throw it
                }
        } else {
            runCatching { backend.synthesize(text, language, voice, speed) }
                .getOrElse { android.synthesize(text, language, voice, speed) }
        }
        AppLog.i(
            "tts_complete",
            mapOf(
                "provider" to result.provider,
                "cached" to result.fromCache,
                "language" to language,
                "bytes" to result.bytes.size,
            ),
        )
        return result
    }
}
