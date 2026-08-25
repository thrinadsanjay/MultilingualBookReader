package com.multilingualbookreader.ocr

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.multilingualbookreader.domain.engine.LanguageDetector
import com.multilingualbookreader.domain.engine.OcrEngine
import com.multilingualbookreader.domain.model.BoundingBox
import com.multilingualbookreader.domain.model.OcrBlock
import com.multilingualbookreader.domain.model.OcrLine
import com.multilingualbookreader.domain.model.OcrResult
import com.multilingualbookreader.domain.model.OcrWord
import com.multilingualbookreader.domain.model.SupportedLanguage
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

/**
 * On-device OCR for Latin (English) and Devanagari (Hindi).
 * Telugu is NOT supported by ML Kit Text Recognition v2 — see BackendOcrEngine.
 */
@Singleton
class MlKitOcrEngine @Inject constructor(
    private val languageDetector: LanguageDetector,
) : OcrEngine {
    override val name: String = "mlkit-ondevice"
    override val supportsOffline: Boolean = true
    override val supportedLanguages: Set<SupportedLanguage> =
        setOf(SupportedLanguage.ENGLISH, SupportedLanguage.HINDI)

    private val latin = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val devanagari = TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())

    override suspend fun recognize(imageBytes: ByteArray, hintLanguage: SupportedLanguage?): OcrResult {
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            ?: return emptyResult()
        return recognizeBitmap(bitmap, hintLanguage)
    }

    suspend fun recognizeBitmap(bitmap: Bitmap, hintLanguage: SupportedLanguage?): OcrResult {
        val image = InputImage.fromBitmap(bitmap, 0)
        val primary = when (hintLanguage) {
            SupportedLanguage.HINDI -> devanagari.process(image).await()
            SupportedLanguage.ENGLISH -> latin.process(image).await()
            // Each recogniser is tried independently: one script's model failing must not discard
            // a page the other script read perfectly well.
            else -> {
                val latinResult = runCatching { latin.process(image).await() }
                val devResult = runCatching { devanagari.process(image).await() }
                val best = listOfNotNull(latinResult.getOrNull(), devResult.getOrNull())
                    .maxByOrNull { it.text.length }
                best ?: throw latinResult.exceptionOrNull() ?: devResult.exceptionOrNull()!!
            }
        }
        return primary.toDomain(name, processedOffline = true, languageDetector)
    }

    private fun emptyResult() = OcrResult(
        text = "",
        language = SupportedLanguage.UNKNOWN,
        confidence = 0f,
        blocks = emptyList(),
        engineName = name,
        processedOffline = true,
    )
}

internal fun Text.toDomain(
    engineName: String,
    processedOffline: Boolean,
    languageDetector: LanguageDetector,
): OcrResult {
    val blocks = textBlocks.map { block ->
        val lines = block.lines.map { line ->
            val words = line.elements.map { element ->
                OcrWord(
                    text = element.text,
                    confidence = runCatching { element.confidence }.getOrDefault(0f),
                    boundingBox = element.boundingBox?.toDomain(),
                    language = languageDetector.detect(element.text),
                )
            }
            OcrLine(
                text = line.text,
                confidence = words.map { it.confidence }.average().let { if (it.isNaN()) 0f else it.toFloat() },
                boundingBox = line.boundingBox?.toDomain(),
                words = words,
                language = languageDetector.detect(line.text),
            )
        }
        OcrBlock(
            text = block.text,
            confidence = lines.map { it.confidence }.average().let { if (it.isNaN()) 0f else it.toFloat() },
            boundingBox = block.boundingBox?.toDomain(),
            lines = lines,
            language = languageDetector.detect(block.text),
        )
    }
    val text = this.text
    return OcrResult(
        text = text,
        language = languageDetector.detect(text),
        confidence = blocks.map { it.confidence }.average().toFloat().takeIf { blocks.isNotEmpty() } ?: 0f,
        blocks = blocks,
        engineName = engineName,
        processedOffline = processedOffline,
    )
}

internal fun android.graphics.Rect.toDomain() = BoundingBox(
    left = left.toFloat(),
    top = top.toFloat(),
    right = right.toFloat(),
    bottom = bottom.toFloat(),
)
