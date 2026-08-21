package com.multilingualbookreader.ocr

import com.multilingualbookreader.common.AppLog
import com.multilingualbookreader.domain.engine.LanguageDetector
import com.multilingualbookreader.domain.engine.OcrEngine
import com.multilingualbookreader.domain.engine.TextProcessor
import com.multilingualbookreader.domain.model.OcrResult
import com.multilingualbookreader.domain.model.OcrRoute
import com.multilingualbookreader.domain.model.SupportedLanguage
import com.multilingualbookreader.domain.repository.SettingsRepository
import com.multilingualbookreader.network.ConnectivityObserver
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CompositeOcrEngine @Inject constructor(
    private val mlKit: MlKitOcrEngine,
    private val backend: BackendOcrEngine,
    private val settings: SettingsRepository,
    private val connectivity: ConnectivityObserver,
    private val textProcessor: TextProcessor,
    private val languageDetector: LanguageDetector,
) : OcrEngine {
    override val name: String = "composite-ocr"
    override val supportsOffline: Boolean = true
    override val supportedLanguages: Set<SupportedLanguage> =
        setOf(SupportedLanguage.ENGLISH, SupportedLanguage.HINDI, SupportedLanguage.TELUGU)

    override suspend fun recognize(imageBytes: ByteArray, hintLanguage: SupportedLanguage?): OcrResult {
        val route = settings.get().ocrRoute
        val result = when {
            hintLanguage == SupportedLanguage.TELUGU && connectivity.isOnline -> backend.recognize(imageBytes, hintLanguage)
            route == OcrRoute.CLOUD && connectivity.isOnline -> backend.recognize(imageBytes, hintLanguage)
            route == OcrRoute.ON_DEVICE -> mlKit.recognize(imageBytes, hintLanguage)
            else -> recognizeAuto(imageBytes, hintLanguage)
        }
        val cleaned = textProcessor.clean(result.text, result.language)
        val language = languageDetector.detect(cleaned).takeIf { it != SupportedLanguage.UNKNOWN } ?: result.language
        AppLog.i("ocr_complete", mapOf("engine" to result.engineName, "language" to language.bcp47, "offline" to result.processedOffline))
        return result.copy(text = cleaned, language = language)
    }

    private suspend fun recognizeAuto(imageBytes: ByteArray, hintLanguage: SupportedLanguage?): OcrResult {
        val local = runCatching { mlKit.recognize(imageBytes, hintLanguage) }.getOrNull()
        val looksTeluguMissing = local == null ||
            local.confidence < 0.45f ||
            local.text.isBlank() ||
            hintLanguage == SupportedLanguage.TELUGU
        if (looksTeluguMissing && connectivity.isOnline) {
            return backend.recognize(imageBytes, hintLanguage)
        }
        if (local != null) return local
        if (connectivity.isOnline) return backend.recognize(imageBytes, hintLanguage)
        error("OCR is unavailable offline for this page.")
    }
}
