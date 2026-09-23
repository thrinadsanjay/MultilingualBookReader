package com.multilingualbookreader.voice

import com.multilingualbookreader.domain.AppError
import com.multilingualbookreader.domain.engine.VoiceCloningEngine
import com.multilingualbookreader.domain.model.SupportedLanguage
import com.multilingualbookreader.domain.model.VoiceProfile
import com.multilingualbookreader.domain.model.VoiceStatus
import com.multilingualbookreader.domain.repository.AudioCacheRepository
import com.multilingualbookreader.domain.repository.VoiceRepository
import com.multilingualbookreader.network.BookReaderApi
import com.multilingualbookreader.network.ConnectivityObserver
import com.multilingualbookreader.storage.LocalFileStore
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

@Singleton
class BackendVoiceCloningEngine @Inject constructor(
    private val api: BookReaderApi,
    private val connectivity: ConnectivityObserver,
    private val voices: VoiceRepository,
    private val audioCache: AudioCacheRepository,
    private val files: LocalFileStore,
) : VoiceCloningEngine {
    override val name: String = "backend-voice"

    override suspend fun createVoice(
        name: String,
        sampleWavFiles: List<ByteArray>,
        consentConfirmed: Boolean,
    ): VoiceProfile {
        if (!consentConfirmed) {
            throw AppError.Generic("Please confirm you are allowed to use this voice.")
        }
        if (sampleWavFiles.isEmpty()) {
            throw AppError.Generic("Record at least one sample, then tap Create voice.")
        }
        val trimmed = name.trim().ifBlank { "My voice" }
        val localId = UUID.randomUUID().toString()
        sampleWavFiles.forEachIndexed { index, bytes -> files.saveVoiceSample(localId, index, bytes) }
        val local = localDraft(localId, trimmed, sampleWavFiles.size)
        voices.upsert(local)
        if (!connectivity.isOnline || sampleWavFiles.size < 3) {
            return local
        }
        return runCatching {
            val parts = sampleWavFiles.mapIndexed { index, bytes ->
                MultipartBody.Part.createFormData(
                    "samples",
                    "sample-$index.m4a",
                    bytes.toRequestBody("audio/mp4".toMediaType()),
                )
            }
            val remote = api.createVoice(
                name = trimmed.toRequestBody("text/plain".toMediaType()),
                consent = true.toString().toRequestBody("text/plain".toMediaType()),
                samples = parts,
            )
            val profile = VoiceProfile(
                id = remote.id,
                name = remote.name.ifBlank { trimmed },
                provider = remote.provider,
                providerVoiceId = remote.providerVoiceId,
                supportedLanguages = remote.supportedLanguages.map { SupportedLanguage.fromBcp47(it) },
                experimentalLanguages = remote.experimentalLanguages.map { SupportedLanguage.fromBcp47(it) },
                status = runCatching { VoiceStatus.valueOf(remote.status.uppercase()) }.getOrDefault(VoiceStatus.READY),
                isCloned = remote.isCloned,
                qualityNote = remote.qualityNote,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
            )
            voices.upsert(profile)
            voices.delete(localId)
            files.deleteVoiceSamples(localId)
            profile
        }.getOrElse { local }
    }

    companion object {
        fun localDraft(id: String, name: String, sampleCount: Int) = VoiceProfile(
            id = id,
            name = name,
            provider = "pending-upload",
            providerVoiceId = null,
            supportedLanguages = emptyList(),
            status = VoiceStatus.DRAFT,
            isCloned = true,
            qualityNote = if (sampleCount < 3) {
                "Saved on this phone with $sampleCount sample${if (sampleCount == 1) "" else "s"}. Record two more if you want the reading server to clone it."
            } else {
                "Saved on this phone. A reading server in Settings can finish cloning this voice. Until then, books use the device voice."
            },
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
        )
    }

    override suspend fun getVoice(id: String): VoiceProfile {
        voices.get(id)?.let { return it }
        if (!connectivity.isOnline) throw AppError.Offline("voice")
        val remote = api.voice(id)
        return VoiceProfile(
            id = remote.id,
            name = remote.name,
            provider = remote.provider,
            providerVoiceId = remote.providerVoiceId,
            supportedLanguages = remote.supportedLanguages.map { SupportedLanguage.fromBcp47(it) },
            experimentalLanguages = remote.experimentalLanguages.map { SupportedLanguage.fromBcp47(it) },
            status = VoiceStatus.READY,
            isCloned = remote.isCloned,
            qualityNote = remote.qualityNote,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
        )
    }

    override suspend fun listVoices(): List<VoiceProfile> = voices.observeProfiles().first()

    override suspend fun deleteVoice(id: String) {
        runCatching { if (connectivity.isOnline) api.deleteVoice(id) }
        audioCache.deleteForVoice(id)
        files.deleteVoiceSamples(id)
        voices.delete(id)
    }
}
