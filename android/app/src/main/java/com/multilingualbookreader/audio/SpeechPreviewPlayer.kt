package com.multilingualbookreader.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

interface SpeechPreviewPlayer {
    fun play(bytes: ByteArray, mimeType: String, onFinished: () -> Unit = {})
    fun stop()
}

@Singleton
class MediaSpeechPreviewPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
) : SpeechPreviewPlayer {
    private var player: MediaPlayer? = null

    override fun play(bytes: ByteArray, mimeType: String, onFinished: () -> Unit) {
        stop()
        if (bytes.isEmpty()) error("No speech was generated.")
        val file = File(context.cacheDir, "voice-preview.${extension(mimeType)}")
        file.writeBytes(bytes)
        val next = MediaPlayer()
        player = next
        next.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build(),
        )
        next.setDataSource(file.absolutePath)
        next.setOnCompletionListener {
            stop()
            Handler(Looper.getMainLooper()).post(onFinished)
        }
        next.setOnErrorListener { _, _, _ ->
            stop()
            Handler(Looper.getMainLooper()).post(onFinished)
            true
        }
        next.prepare()
        next.start()
    }

    override fun stop() {
        val current = player ?: return
        player = null
        runCatching { if (current.isPlaying) current.stop() }
        current.reset()
        current.release()
    }

    private fun extension(mimeType: String): String = when {
        mimeType.contains("wav", ignoreCase = true) -> "wav"
        mimeType.contains("mp3", ignoreCase = true) || mimeType.contains("mpeg", ignoreCase = true) -> "mp3"
        else -> "bin"
    }
}
