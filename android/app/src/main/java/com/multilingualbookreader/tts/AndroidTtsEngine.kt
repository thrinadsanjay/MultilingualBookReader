package com.multilingualbookreader.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.multilingualbookreader.audio.AudioCacheKey
import com.multilingualbookreader.domain.engine.TextToSpeechEngine
import com.multilingualbookreader.domain.model.AudioResult
import com.multilingualbookreader.domain.model.SupportedLanguage
import com.multilingualbookreader.domain.model.VoiceProfile
import com.multilingualbookreader.storage.LocalFileStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Offline fallback using the device speech engine.
 * Quality for Telugu and Hindi depends entirely on whether the user
 * has installed those voices in Android settings. This is not the
 * production-quality path.
 */
@Singleton
class AndroidTtsEngine @Inject constructor(
    @ApplicationContext context: Context,
    private val files: LocalFileStore,
) : TextToSpeechEngine {
    override val name: String = "android-tts"
    override val supportsOffline: Boolean = true

    private val ready = AtomicBoolean(false)
    private val mutex = Mutex()
    private val tts: TextToSpeech = TextToSpeech(context) { status ->
        ready.set(status == TextToSpeech.SUCCESS)
    }

    override suspend fun synthesize(
        text: String,
        language: String,
        voice: VoiceProfile,
        speed: Float,
    ): AudioResult = mutex.withLock {
        val prepared = withTimeoutOrNull(8_000) {
            while (!ready.get()) delay(50)
            true
        } == true
        if (!prepared) error("Device speech is not ready.")
        val locale = localeFor(language)
        tts.language = locale
        tts.setSpeechRate(speed.coerceIn(0.5f, 2.0f))
        val cacheKey = AudioCacheKey.of(text, language, voice.id, speed, name)
        val outFile = files.audioFile(cacheKey, "wav")
        if (outFile.exists() && outFile.length() > 0) {
            return@withLock AudioResult(outFile.readBytes(), "audio/wav", 0, cacheKey, true, name)
        }
        synthesizeToFile(text, outFile)
        AudioResult(outFile.readBytes(), "audio/wav", 0, cacheKey, false, name)
    }

    private suspend fun synthesizeToFile(text: String, file: File) = suspendCancellableCoroutine { cont ->
        val utteranceId = UUID.randomUUID().toString()
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) {
                if (cont.isActive) cont.resume(Unit)
            }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                if (cont.isActive) cont.resumeWithException(IllegalStateException("Device speech failed."))
            }
            override fun onError(utteranceId: String?, errorCode: Int) {
                if (cont.isActive) cont.resumeWithException(IllegalStateException("Device speech failed ($errorCode)."))
            }
        })
        val result = tts.synthesizeToFile(text, null, file, utteranceId)
        if (result != TextToSpeech.SUCCESS && cont.isActive) {
            cont.resumeWithException(IllegalStateException("Device speech rejected the request."))
        }
    }

    private fun localeFor(language: String): Locale = when (SupportedLanguage.fromBcp47(language)) {
        SupportedLanguage.HINDI -> Locale("hi", "IN")
        SupportedLanguage.TELUGU -> Locale("te", "IN")
        else -> Locale.US
    }
}
