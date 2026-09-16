package com.multilingualbookreader.audio

import java.security.MessageDigest

object AudioCacheKey {
    fun of(
        text: String,
        language: String,
        voiceId: String,
        speed: Float,
        provider: String = "",
    ): String {
        val normalizedSpeed = "%.2f".format(speed)
        val material = listOf(
            text.trim(),
            language.lowercase(),
            voiceId,
            normalizedSpeed,
            provider,
        ).joinToString("|")
        return sha256(material)
    }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }
}
