package com.multilingualbookreader.domain.model

enum class SupportedLanguage(val bcp47: String, val displayName: String) {
    ENGLISH("en", "English"),
    HINDI("hi", "Hindi"),
    TELUGU("te", "Telugu"),
    MIXED("mul", "Mixed"),
    UNKNOWN("und", "Unknown");

    companion object {
        fun fromBcp47(code: String?): SupportedLanguage {
            val normalized = code?.lowercase()?.substringBefore('-').orEmpty()
            return entries.firstOrNull { it.bcp47 == normalized } ?: UNKNOWN
        }
    }
}

enum class BookSource { CAMERA_SCAN, PDF, MIXED }

enum class ProcessingStatus { PENDING, PROCESSING, COMPLETED, FAILED, RETRYING }

enum class VoiceStatus { DRAFT, UPLOADING, VERIFYING, READY, FAILED, DELETED }

enum class AudioStatus { PENDING, GENERATING, READY, FAILED }

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class OcrRoute { ON_DEVICE, CLOUD, AUTO }
