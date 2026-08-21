package com.multilingualbookreader.network

import com.multilingualbookreader.domain.model.SupportedLanguage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface BookReaderApi {
    @POST("api/v1/auth/register")
    suspend fun register(@Body body: AuthRequest): AuthResponse

    @POST("api/v1/auth/login")
    suspend fun login(@Body body: AuthRequest): AuthResponse

    @Multipart
    @POST("api/v1/ocr")
    suspend fun ocr(
        @Part image: MultipartBody.Part,
        @Part("hint_language") hint: RequestBody?,
    ): RemoteOcrResult

    @POST("api/v1/tts")
    suspend fun tts(@Body body: TtsRequest): ResponseBody

    @POST("api/v1/audio/generate")
    suspend fun generateAudio(@Body body: TtsRequest): AudioGenerateResponse

    @GET("api/v1/audio/{id}")
    suspend fun getAudio(@Path("id") id: String): ResponseBody

    @Multipart
    @POST("api/v1/voice/create")
    suspend fun createVoice(
        @Part("name") name: RequestBody,
        @Part("consent") consent: RequestBody,
        @Part samples: List<MultipartBody.Part>,
    ): RemoteVoiceProfile

    @GET("api/v1/voices")
    suspend fun voices(): List<RemoteVoiceProfile>

    @GET("api/v1/voice/{id}")
    suspend fun voice(@Path("id") id: String): RemoteVoiceProfile

    @DELETE("api/v1/voice/{id}")
    suspend fun deleteVoice(@Path("id") id: String)

    @GET("api/v1/status")
    suspend fun status(): ProviderStatus
}

@Serializable
data class AuthRequest(val email: String, val password: String)

@Serializable
data class AuthResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    val email: String,
)

@Serializable
data class TtsRequest(
    val text: String,
    val language: String,
    @SerialName("voice_id") val voiceId: String? = null,
    val speed: Float = 1.0f,
)

@Serializable
data class AudioGenerateResponse(
    val id: String,
    val cached: Boolean,
    @SerialName("duration_ms") val durationMs: Long = 0,
    @SerialName("mime_type") val mimeType: String = "audio/mpeg",
)

@Serializable
data class RemoteOcrResult(
    val text: String,
    val language: String,
    val confidence: Float,
    val blocks: List<RemoteOcrBlock> = emptyList(),
    val engine: String = "unknown",
)

@Serializable
data class RemoteOcrBlock(
    val text: String,
    val confidence: Float = 0f,
    val language: String? = null,
    @SerialName("bounding_box") val boundingBox: RemoteBox? = null,
    val lines: List<RemoteOcrLine> = emptyList(),
)

@Serializable
data class RemoteOcrLine(
    val text: String,
    val confidence: Float = 0f,
    @SerialName("bounding_box") val boundingBox: RemoteBox? = null,
    val words: List<RemoteOcrWord> = emptyList(),
)

@Serializable
data class RemoteOcrWord(
    val text: String,
    val confidence: Float = 0f,
    @SerialName("bounding_box") val boundingBox: RemoteBox? = null,
)

@Serializable
data class RemoteBox(val left: Float, val top: Float, val right: Float, val bottom: Float)

@Serializable
data class RemoteVoiceProfile(
    val id: String,
    val name: String,
    val provider: String,
    @SerialName("provider_voice_id") val providerVoiceId: String? = null,
    @SerialName("supported_languages") val supportedLanguages: List<String> = emptyList(),
    @SerialName("experimental_languages") val experimentalLanguages: List<String> = emptyList(),
    val status: String,
    @SerialName("is_cloned") val isCloned: Boolean = true,
    @SerialName("quality_note") val qualityNote: String? = null,
)

@Serializable
data class ProviderStatus(
    val online: Boolean = true,
    @SerialName("ocr_provider") val ocrProvider: String = "",
    @SerialName("tts_provider") val ttsProvider: String = "",
    @SerialName("voice_provider") val voiceProvider: String = "",
)

fun String.toLanguage(): SupportedLanguage = SupportedLanguage.fromBcp47(this)
