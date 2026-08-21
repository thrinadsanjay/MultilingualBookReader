package com.multilingualbookreader.domain.model

data class AudioResult(
    val bytes: ByteArray,
    val mimeType: String,
    val durationMs: Long,
    val cacheKey: String,
    val fromCache: Boolean,
    val provider: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AudioResult) return false
        return mimeType == other.mimeType &&
            durationMs == other.durationMs &&
            cacheKey == other.cacheKey &&
            fromCache == other.fromCache &&
            provider == other.provider &&
            bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + durationMs.hashCode()
        result = 31 * result + cacheKey.hashCode()
        result = 31 * result + fromCache.hashCode()
        result = 31 * result + provider.hashCode()
        return result
    }
}

data class TextSegment(
    val id: String,
    val text: String,
    val language: SupportedLanguage,
    val startOffset: Int,
    val endOffset: Int,
)

data class LanguageSpan(
    val text: String,
    val language: SupportedLanguage,
    val startOffset: Int,
    val endOffset: Int,
)

data class VoiceSampleRequirements(
    val minDurationMs: Long = 8_000,
    val recommendedDurationMs: Long = 20_000,
    val minSampleCount: Int = 3,
    val recommendedDistanceCm: IntRange = 15..20,
)
