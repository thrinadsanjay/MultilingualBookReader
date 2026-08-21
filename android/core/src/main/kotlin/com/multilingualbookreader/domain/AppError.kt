package com.multilingualbookreader.domain

sealed class AppError(
    override val message: String,
    override val cause: Throwable? = null,
    val userMessage: String,
) : Exception(message, cause) {
    class Offline(feature: String) : AppError(
        message = "offline:$feature",
        userMessage = "You are offline. This action needs an internet connection.",
    )

    class OcrFailed(cause: Throwable? = null) : AppError(
        message = "ocr_failed",
        cause = cause,
        userMessage = "We could not read this page. Try a clearer photo or a different page.",
    )

    class TtsFailed(cause: Throwable? = null) : AppError(
        message = "tts_failed",
        cause = cause,
        userMessage = "Voice playback could not be generated. Try again in a moment.",
    )

    class VoiceFailed(cause: Throwable? = null) : AppError(
        message = "voice_failed",
        cause = cause,
        userMessage = "Your voice profile could not be created.",
    )

    class InvalidPdf : AppError(
        message = "invalid_pdf",
        userMessage = "This file is not a readable PDF.",
    )

    class UnsupportedLanguage(language: String) : AppError(
        message = "unsupported_language:$language",
        userMessage = "This language is not supported yet.",
    )

    class ProviderUnavailable(provider: String) : AppError(
        message = "provider_unavailable:$provider",
        userMessage = "Speech or scanning is temporarily unavailable.",
    )

    class RateLimited : AppError(
        message = "rate_limited",
        userMessage = "Too many requests. Please wait a little and try again.",
    )

    class InsufficientStorage : AppError(
        message = "insufficient_storage",
        userMessage = "There is not enough storage to save this book or audio.",
    )

    class CorruptedBook : AppError(
        message = "corrupted_book",
        userMessage = "This book looks damaged. You can try importing it again.",
    )

    class AuthFailed : AppError(
        message = "auth_failed",
        userMessage = "Sign-in failed. Check your email and password.",
    )

    class Generic(userMessage: String, cause: Throwable? = null) : AppError(
        message = "generic",
        cause = cause,
        userMessage = userMessage,
    )
}
