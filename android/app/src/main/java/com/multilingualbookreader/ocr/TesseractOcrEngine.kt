package com.multilingualbookreader.ocr

import android.content.Context
import android.graphics.BitmapFactory
import com.googlecode.tesseract.android.TessBaseAPI
import com.multilingualbookreader.domain.engine.LanguageDetector
import com.multilingualbookreader.domain.engine.OcrEngine
import com.multilingualbookreader.domain.model.OcrResult
import com.multilingualbookreader.domain.model.SupportedLanguage
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * On-device Tesseract with bundled English + Telugu models. ML Kit cannot read Telugu; this can,
 * so a reading server is optional rather than required.
 */
@Singleton
class TesseractOcrEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val languageDetector: LanguageDetector,
) : OcrEngine {
    override val name: String = "tesseract-ondevice"
    override val supportsOffline: Boolean = true
    override val supportedLanguages: Set<SupportedLanguage> =
        setOf(SupportedLanguage.ENGLISH, SupportedLanguage.TELUGU)

    private val mutex = Mutex()

    override suspend fun recognize(imageBytes: ByteArray, hintLanguage: SupportedLanguage?): OcrResult {
        return withContext(Dispatchers.IO) {
            mutex.withLock {
                runCatching { recognizeLocked(imageBytes, hintLanguage) }.getOrElse { emptyResult() }
            }
        }
    }

    private fun recognizeLocked(imageBytes: ByteArray, hintLanguage: SupportedLanguage?): OcrResult {
        installModels()
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size) ?: return emptyResult()
        val tess = TessBaseAPI()
        try {
            val languages = if (hintLanguage == SupportedLanguage.TELUGU) "tel+eng" else "eng+tel"
            if (!tess.init(context.filesDir.absolutePath, languages)) return emptyResult()
            tess.setImage(bitmap)
            val text = tess.utF8Text.orEmpty().trim()
            return OcrResult(
                text = text,
                language = languageDetector.detect(text),
                confidence = 0f,
                blocks = emptyList(),
                engineName = name,
                processedOffline = true,
            )
        } finally {
            tess.recycle()
        }
    }

    private fun installModels() {
        val dir = File(context.filesDir, "tessdata")
        if (!dir.exists()) dir.mkdirs()
        MODELS.forEach { name ->
            val dest = File(dir, name)
            if (dest.exists() && dest.length() > 0L) return@forEach
            context.assets.open("tessdata/$name").use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
        }
    }

    private fun emptyResult() = OcrResult(
        text = "",
        language = SupportedLanguage.UNKNOWN,
        confidence = 0f,
        blocks = emptyList(),
        engineName = name,
        processedOffline = true,
    )

    private companion object {
        val MODELS = listOf("eng.traineddata", "tel.traineddata")
    }
}
