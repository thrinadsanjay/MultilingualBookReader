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
    private val tesseract: TesseractOcrEngine,
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
        val result = when (settings.get().ocrRoute) {
            OcrRoute.CLOUD -> cloudFirst(imageBytes, hintLanguage)
            OcrRoute.ON_DEVICE -> {
                val local = recognizeLocally(imageBytes, hintLanguage)
                val tess = recognizeWithTesseract(imageBytes, hintLanguage, local?.text)
                pickOnDevice(local, tess) ?: local ?: error("This page could not be read.")
            }
            OcrRoute.AUTO -> deviceFirst(imageBytes, hintLanguage)
        }
        val cleaned = textProcessor.clean(result.text, result.language)
        val language = languageDetector.detect(cleaned).takeIf { it != SupportedLanguage.UNKNOWN } ?: result.language
        AppLog.i(
            "ocr_complete",
            mapOf("engine" to result.engineName, "language" to language.bcp47, "offline" to result.processedOffline),
        )
        return result.copy(text = cleaned, language = language)
    }

    /** Reads on the phone first (ML Kit, then Tesseract for Telugu) and only then the server. */
    private suspend fun deviceFirst(imageBytes: ByteArray, hintLanguage: SupportedLanguage?): OcrResult {
        val local = recognizeLocally(imageBytes, hintLanguage)
        val tess = recognizeWithTesseract(imageBytes, hintLanguage, local?.text)
        val onDevice = pickOnDevice(local, tess)
        val inferredHint = hintLanguage
            ?: SupportedLanguage.TELUGU.takeIf { onDevice?.text?.let(OcrFallbackPolicy::containsTelugu) == true }
        val askBackend = OcrFallbackPolicy.shouldTryBackend(onDevice?.text, inferredHint, connectivity.isOnline)
        val remote = if (askBackend) recognizeRemotely(imageBytes, inferredHint) else null
        return pick(onDevice, remote, askBackend)
    }

    private suspend fun cloudFirst(imageBytes: ByteArray, hintLanguage: SupportedLanguage?): OcrResult {
        val remote = if (connectivity.isOnline) recognizeRemotely(imageBytes, hintLanguage) else null
        if (remote != null && remote.text.isNotBlank() && !OcrFallbackPolicy.looksUnreliable(remote.text)) return remote
        val local = recognizeLocally(imageBytes, hintLanguage)
        val tess = recognizeWithTesseract(imageBytes, hintLanguage, local?.text)
        val onDevice = pickOnDevice(local, tess)
        return pick(onDevice, remote, backendAttempted = connectivity.isOnline)
    }

    private fun pickOnDevice(mlKit: OcrResult?, tess: OcrResult?): OcrResult? {
        val winner = OcrFallbackPolicy.better(mlKit?.text, tess?.text) ?: return mlKit ?: tess
        return listOfNotNull(mlKit, tess).firstOrNull { it.text == winner } ?: mlKit ?: tess
    }

    private suspend fun recognizeWithTesseract(
        imageBytes: ByteArray,
        hintLanguage: SupportedLanguage?,
        localText: String?,
    ): OcrResult? {
        val tryTess = hintLanguage == SupportedLanguage.TELUGU ||
            localText.isNullOrBlank() ||
            OcrFallbackPolicy.looksUnreliable(localText) ||
            OcrFallbackPolicy.containsTelugu(localText)
        if (!tryTess) return null
        return runCatching { tesseract.recognize(imageBytes, hintLanguage ?: SupportedLanguage.TELUGU) }
            .onFailure { AppLog.w("ocr_tesseract_failed") }
            .getOrNull()
    }

    private fun pick(local: OcrResult?, remote: OcrResult?, backendAttempted: Boolean): OcrResult {
        return when (OcrFallbackPolicy.choose(local?.text, remote?.text, backendAttempted)) {
            OcrSource.BACKEND -> remote ?: error("no backend result")
            OcrSource.LOCAL -> local ?: error("no local result")
            null -> error("This page could not be read.")
        }
    }

    private suspend fun recognizeLocally(imageBytes: ByteArray, hintLanguage: SupportedLanguage?): OcrResult? =
        runCatching { mlKit.recognize(imageBytes, hintLanguage) }
            .onFailure { AppLog.w("ocr_on_device_failed") }
            .getOrNull()

    /** A server that is missing or unreachable is normal here, so it must never abort the read. */
    private suspend fun recognizeRemotely(imageBytes: ByteArray, hintLanguage: SupportedLanguage?): OcrResult? =
        runCatching { backend.recognize(imageBytes, hintLanguage) }
            .onFailure { AppLog.w("ocr_backend_unavailable") }
            .getOrNull()
}
