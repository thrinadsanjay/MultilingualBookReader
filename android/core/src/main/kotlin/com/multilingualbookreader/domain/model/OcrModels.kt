package com.multilingualbookreader.domain.model

data class BoundingBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

data class OcrWord(
    val text: String,
    val confidence: Float,
    val boundingBox: BoundingBox?,
    val language: SupportedLanguage = SupportedLanguage.UNKNOWN,
)

data class OcrLine(
    val text: String,
    val confidence: Float,
    val boundingBox: BoundingBox?,
    val words: List<OcrWord>,
    val language: SupportedLanguage = SupportedLanguage.UNKNOWN,
)

data class OcrBlock(
    val text: String,
    val confidence: Float,
    val boundingBox: BoundingBox?,
    val lines: List<OcrLine>,
    val language: SupportedLanguage = SupportedLanguage.UNKNOWN,
)

data class OcrResult(
    val text: String,
    val language: SupportedLanguage,
    val confidence: Float,
    val blocks: List<OcrBlock>,
    val engineName: String,
    val processedOffline: Boolean,
)
