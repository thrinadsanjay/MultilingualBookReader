package com.multilingualbookreader.domain.engine

import com.multilingualbookreader.domain.model.AudioResult
import com.multilingualbookreader.domain.model.LanguageSpan
import com.multilingualbookreader.domain.model.OcrResult
import com.multilingualbookreader.domain.model.SupportedLanguage
import com.multilingualbookreader.domain.model.VoiceProfile

interface OcrEngine {
    val name: String
    val supportsOffline: Boolean
    val supportedLanguages: Set<SupportedLanguage>
    suspend fun recognize(imageBytes: ByteArray, hintLanguage: SupportedLanguage? = null): OcrResult
}

interface TextToSpeechEngine {
    val name: String
    val supportsOffline: Boolean
    suspend fun synthesize(
        text: String,
        language: String,
        voice: VoiceProfile,
        speed: Float = 1.0f,
    ): AudioResult
}

interface VoiceCloningEngine {
    val name: String
    suspend fun createVoice(
        name: String,
        sampleWavFiles: List<ByteArray>,
        consentConfirmed: Boolean,
    ): VoiceProfile

    suspend fun getVoice(id: String): VoiceProfile
    suspend fun listVoices(): List<VoiceProfile>
    suspend fun deleteVoice(id: String)
}

interface LanguageDetector {
    fun detect(text: String): SupportedLanguage
    fun detectSpans(text: String): List<LanguageSpan>
}

interface TextProcessor {
    fun clean(raw: String, language: SupportedLanguage = SupportedLanguage.UNKNOWN): String
    fun prepareForSpeech(text: String, language: SupportedLanguage): String
}
