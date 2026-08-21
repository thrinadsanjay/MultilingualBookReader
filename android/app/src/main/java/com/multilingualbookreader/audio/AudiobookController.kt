package com.multilingualbookreader.audio

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.multilingualbookreader.audio.playback.BookPlaybackService
import com.multilingualbookreader.domain.engine.TextToSpeechEngine
import com.multilingualbookreader.domain.model.AudioSegment
import com.multilingualbookreader.domain.model.AudioStatus
import com.multilingualbookreader.domain.model.ReadingProgress
import com.multilingualbookreader.domain.model.TextSegment
import com.multilingualbookreader.domain.model.VoiceProfile
import com.multilingualbookreader.domain.repository.AudioCacheRepository
import com.multilingualbookreader.domain.repository.ProgressRepository
import com.multilingualbookreader.storage.LocalFileStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

data class PlaybackState(
    val isPlaying: Boolean = false,
    val currentSegmentIndex: Int = 0,
    val segments: List<TextSegment> = emptyList(),
    val speed: Float = 1.0f,
    val pageNumber: Int = 1,
)

@Singleton
class AudiobookController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tts: TextToSpeechEngine,
    private val cache: AudioCacheRepository,
    private val files: LocalFileStore,
    private val progress: ProgressRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state
    private var controller: MediaController? = null
    private var bookId: String? = null
    private var pageId: String = ""

    suspend fun prepare(
        bookId: String,
        pageId: String,
        pageNumber: Int,
        segments: List<TextSegment>,
        voice: VoiceProfile,
        speed: Float,
    ) {
        this.bookId = bookId
        this.pageId = pageId
        val paths = segments.mapIndexed { index, segment -> ensureAudio(segment, index, voice, speed) }
        val mediaController = ensureController()
        mediaController.setMediaItems(paths.map { MediaItem.fromUri(android.net.Uri.fromFile(File(it))) }, 0, 0L)
        mediaController.setPlaybackSpeed(speed)
        mediaController.prepare()
        _state.value = PlaybackState(false, 0, segments, speed, pageNumber)
        mediaController.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val index = mediaController.currentMediaItemIndex
                _state.value = _state.value.copy(currentSegmentIndex = index)
                persistProgress(index)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.value = _state.value.copy(isPlaying = isPlaying)
            }
        })
    }

    fun play() { controller?.play() }
    fun pause() { controller?.pause() }
    fun next() { controller?.seekToNextMediaItem() }
    fun previous() { controller?.seekToPreviousMediaItem() }
    fun setSpeed(speed: Float) {
        controller?.setPlaybackSpeed(speed)
        _state.value = _state.value.copy(speed = speed)
    }
    fun skipParagraph(segmentsPerParagraph: Int = 3) {
        val last = _state.value.segments.lastIndex.coerceAtLeast(0)
        val next = (_state.value.currentSegmentIndex + segmentsPerParagraph).coerceAtMost(last)
        controller?.seekToDefaultPosition(next)
    }
    fun repeatSentence() {
        controller?.seekToDefaultPosition(_state.value.currentSegmentIndex)
        controller?.play()
    }
    fun restartPage() {
        controller?.seekToDefaultPosition(0)
        controller?.play()
    }

    private suspend fun ensureAudio(segment: TextSegment, index: Int, voice: VoiceProfile, speed: Float): String {
        val result = tts.synthesize(segment.text, segment.language.bcp47, voice, speed)
        val extension = if (result.mimeType.contains("wav")) "wav" else "mp3"
        val file = files.audioFile(result.cacheKey, extension)
        if (!file.exists()) files.saveAudio(result.cacheKey, result.bytes, extension)
        cache.put(
            AudioSegment(
                id = UUID.randomUUID().toString(),
                bookPageId = pageId,
                sequence = index,
                text = segment.text,
                language = segment.language,
                audioPath = file.absolutePath,
                durationMs = result.durationMs,
                voiceProfileId = voice.id,
                status = AudioStatus.READY,
                cacheKey = result.cacheKey,
            ),
        )
        return file.absolutePath
    }

    private fun persistProgress(index: Int) {
        val id = bookId ?: return
        val segment = _state.value.segments.getOrNull(index)
        scope.launch(Dispatchers.IO) {
            progress.save(
                ReadingProgress(
                    bookId = id,
                    pageNumber = _state.value.pageNumber,
                    segmentId = segment?.id,
                    characterOffset = segment?.startOffset ?: 0,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    private suspend fun ensureController(): MediaController {
        controller?.let { return it }
        val token = SessionToken(context, ComponentName(context, BookPlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        val created = suspendCancellableCoroutine { cont ->
            future.addListener(
                { if (cont.isActive) cont.resume(future.get()) },
                MoreExecutors.directExecutor(),
            )
        }
        controller = created
        return created
    }
}
