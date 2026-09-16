package com.multilingualbookreader.ocr

import com.multilingualbookreader.domain.AppError
import com.multilingualbookreader.domain.engine.LanguageDetector
import com.multilingualbookreader.domain.engine.OcrEngine
import com.multilingualbookreader.domain.model.BoundingBox
import com.multilingualbookreader.domain.model.OcrBlock
import com.multilingualbookreader.domain.model.OcrLine
import com.multilingualbookreader.domain.model.OcrResult
import com.multilingualbookreader.domain.model.OcrWord
import com.multilingualbookreader.domain.model.SupportedLanguage
import com.multilingualbookreader.network.BookReaderApi
import com.multilingualbookreader.network.ConnectivityObserver
import com.multilingualbookreader.network.RemoteOcrResult
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

@Singleton
class BackendOcrEngine @Inject constructor(
    private val api: BookReaderApi,
    private val connectivity: ConnectivityObserver,
    private val languageDetector: LanguageDetector,
) : OcrEngine {
    override val name: String = "backend-ocr"
    override val supportsOffline: Boolean = false
    override val supportedLanguages: Set<SupportedLanguage> =
        setOf(SupportedLanguage.ENGLISH, SupportedLanguage.HINDI, SupportedLanguage.TELUGU)

    override suspend fun recognize(imageBytes: ByteArray, hintLanguage: SupportedLanguage?): OcrResult {
        if (!connectivity.isOnline) throw AppError.Offline("ocr")
        val imagePart = MultipartBody.Part.createFormData(
            "image",
            "page.jpg",
            imageBytes.toRequestBody("image/jpeg".toMediaType()),
        )
        val hint = hintLanguage?.bcp47?.toRequestBody("text/plain".toMediaType())
        val remote = api.ocr(imagePart, hint)
        return remote.toDomain(languageDetector)
    }
}

internal fun RemoteOcrResult.toDomain(languageDetector: LanguageDetector): OcrResult {
    val blocks = blocks.map { block ->
        OcrBlock(
            text = block.text,
            confidence = block.confidence,
            boundingBox = block.boundingBox?.let { BoundingBox(it.left, it.top, it.right, it.bottom) },
            language = languageDetector.detect(block.text),
            lines = block.lines.map { line ->
                OcrLine(
                    text = line.text,
                    confidence = line.confidence,
                    boundingBox = line.boundingBox?.let { BoundingBox(it.left, it.top, it.right, it.bottom) },
                    language = languageDetector.detect(line.text),
                    words = line.words.map { word ->
                        OcrWord(
                            text = word.text,
                            confidence = word.confidence,
                            boundingBox = word.boundingBox?.let { BoundingBox(it.left, it.top, it.right, it.bottom) },
                            language = languageDetector.detect(word.text),
                        )
                    },
                )
            },
        )
    }
    return OcrResult(
        text = text,
        language = languageDetector.detect(text).takeIf { it != SupportedLanguage.UNKNOWN }
            ?: SupportedLanguage.fromBcp47(language),
        confidence = confidence,
        blocks = blocks,
        engineName = engine,
        processedOffline = false,
    )
}
