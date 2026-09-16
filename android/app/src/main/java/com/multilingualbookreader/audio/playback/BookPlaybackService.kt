package com.multilingualbookreader.audio.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.multilingualbookreader.MainActivity

class BookPlaybackService : MediaSessionService() {
    private var session: MediaSession? = null
    private var player: ExoPlayer? = null

    override fun onCreate() {
        super.onCreate()
        val exo = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true,
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
        exo.playWhenReady = true
        player = exo
        val launch = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        session = MediaSession.Builder(this, exo)
            .setSessionActivity(launch)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onDestroy() {
        session?.release()
        player?.release()
        super.onDestroy()
    }

    companion object {
        fun playFiles(player: Player, paths: List<String>, startIndex: Int, speed: Float) {
            player.setMediaItems(
                paths.map { path ->
                    MediaItem.Builder().setUri(android.net.Uri.fromFile(java.io.File(path))).build()
                },
                startIndex,
                0L,
            )
            player.setPlaybackSpeed(speed)
            player.prepare()
            player.play()
        }
    }
}
