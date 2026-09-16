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

enum class BookPriority(val label: String) {
    LOW("Low"),
    NORMAL("Normal"),
    HIGH("High"),
    URGENT("Urgent");

    companion object {
        fun fromStored(value: String?): BookPriority =
            entries.firstOrNull { it.name == value } ?: NORMAL
    }
}

object BookLooks {
    val colors = listOf("#E8A87C", "#6FBFA8", "#E2C36C", "#9B8EC4", "#E07A9A", "#7EB6D9", "#C27B5A", "#8AA36F")
    val genres = listOf("Fiction", "Textbook", "Religion", "Notes", "Work", "Other")

    fun colorFor(id: String): String {
        val index = (id.hashCode().toLong() and 0x7fffffffL) % colors.size
        return colors[index.toInt()]
    }
}

enum class ProcessingStatus { PENDING, PROCESSING, COMPLETED, FAILED, RETRYING }

enum class VoiceStatus { DRAFT, UPLOADING, VERIFYING, READY, FAILED, DELETED }

enum class AudioStatus { PENDING, GENERATING, READY, FAILED }

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class OcrRoute { ON_DEVICE, CLOUD, AUTO }
