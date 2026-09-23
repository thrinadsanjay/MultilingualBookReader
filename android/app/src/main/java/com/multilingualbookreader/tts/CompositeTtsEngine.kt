package com.multilingualbookreader.tts

import com.multilingualbookreader.BuildConfig
import com.multilingualbookreader.common.AppLog
import com.multilingualbookreader.domain.engine.TextToSpeechEngine
import com.multilingualbookreader.domain.model.AudioResult
import com.multilingualbookreader.domain.model.VoiceProfile
import com.multilingualbookreader.network.BackendUrl
import com.multilingualbookreader.network.ConnectivityObserver
import com.multilingualbookreader.network.EncryptedTokenStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CompositeTtsEngine @Inject constructor(
    private val backend: BackendTtsEngine,
    private val android: AndroidTtsEngine,
    private val connectivity: ConnectivityObserver,
    private val tokens: EncryptedTokenStore,
) : TextToSpeechEngine {
    override val name: String = "composite-tts"
    override val supportsOffline: Boolean = true

    override suspend fun synthesize(
        text: String,
        language: String,
        voice: VoiceProfile,
        speed: Float,
    ): AudioResult {
        val serverConfigured = !tokens.serverUrl().isNullOrBlank() || BackendUrl.isPacked(BuildConfig.API_BASE_URL)
        val preferDevice = prefersDeviceSpeech(
            voice = voice,
            online = connectivity.isOnline,
            serverConfigured = serverConfigured,
        )
        val result = if (preferDevice) {
            runCatching { android.synthesize(text, language, voice, speed) }
                .getOrElse {
                    val canAskServer = connectivity.isOnline &&
                        serverConfigured &&
                        !voice.usesOnDeviceRecording()
                    if (canAskServer) backend.synthesize(text, language, voice, speed) else throw it
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
