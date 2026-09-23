package com.multilingualbookreader.tts

import com.multilingualbookreader.domain.model.VoiceProfile
import com.multilingualbookreader.domain.model.VoiceStatus

/** A recording stored on this phone that is not a cloned TTS voice yet. */
fun VoiceProfile.usesOnDeviceRecording(): Boolean =
    provider == "pending-upload" ||
        provider == "local-recording" ||
        status == VoiceStatus.DRAFT ||
        (isCloned && providerVoiceId.isNullOrBlank() && provider != "backend-tts")

/**
 * Local recordings and phones with no reading server should speak immediately
 * on the device. Only a cloned or standard voice with a configured server
 * should wait on the backend.
 */
fun prefersDeviceSpeech(
    voice: VoiceProfile,
    online: Boolean,
    serverConfigured: Boolean,
): Boolean {
    if (voice.provider == "android-tts") return true
    if (voice.usesOnDeviceRecording()) return true
    return !online || !serverConfigured
}
