package com.multilingualbookreader.presentation.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.multilingualbookreader.audio.AudiobookController
import com.multilingualbookreader.audio.PlaybackState
import com.multilingualbookreader.domain.model.Book
import com.multilingualbookreader.domain.model.BookPage
import com.multilingualbookreader.domain.model.Bookmark
import com.multilingualbookreader.domain.model.Note
import com.multilingualbookreader.domain.model.ReadingProgress
import com.multilingualbookreader.domain.model.SupportedLanguage
import com.multilingualbookreader.domain.model.VoiceProfile
import com.multilingualbookreader.domain.model.VoiceStatus
import com.multilingualbookreader.domain.repository.BookRepository
import com.multilingualbookreader.domain.repository.BookmarkRepository
import com.multilingualbookreader.domain.repository.NoteRepository
import com.multilingualbookreader.domain.repository.ProgressRepository
import com.multilingualbookreader.domain.repository.SettingsRepository
import com.multilingualbookreader.domain.repository.VoiceRepository
import com.multilingualbookreader.text.SentenceSegmenter
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReaderUiState(
    val book: Book? = null,
    val pages: List<BookPage> = emptyList(),
    val pageIndex: Int = 0,
    val playback: PlaybackState = PlaybackState(),
    val bookmarks: List<Bookmark> = emptyList(),
    val notes: List<Note> = emptyList(),
    val error: String? = null,
    val loadingSpeech: Boolean = false,
    val selectedVoice: VoiceProfile = defaultStandardVoice(),
    val noteDraft: String = "",
)

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val books: BookRepository,
    private val progress: ProgressRepository,
    private val bookmarks: BookmarkRepository,
    private val notes: NoteRepository,
    private val voices: VoiceRepository,
    private val settings: SettingsRepository,
    private val segmenter: SentenceSegmenter,
    private val playback: AudiobookController,
) : ViewModel() {
    private val _ui = MutableStateFlow(ReaderUiState())
    val ui: StateFlow<ReaderUiState> = _ui

    init {
        viewModelScope.launch {
            playback.state.collect { play -> _ui.update { it.copy(playback = play) } }
        }
        viewModelScope.launch {
            val selectedId = settings.get().selectedVoiceId
            val profile = selectedId?.let { voices.get(it) } ?: defaultStandardVoice()
            _ui.update { it.copy(selectedVoice = profile) }
        }
    }

    fun load(id: String) {
        viewModelScope.launch {
            val book = books.getBook(id)
            val pages = books.getPages(id)
            val saved = progress.get(id)
            val index = ((saved?.pageNumber ?: 1) - 1).coerceIn(0, (pages.size - 1).coerceAtLeast(0))
            _ui.update { it.copy(book = book, pages = pages, pageIndex = index) }
        }
        viewModelScope.launch {
            bookmarks.observe(id).collect { marks -> _ui.update { it.copy(bookmarks = marks) } }
        }
        viewModelScope.launch {
            notes.observe(id).collect { list -> _ui.update { it.copy(notes = list) } }
        }
    }

    fun goToPage(index: Int) {
        val pages = _ui.value.pages
        if (pages.isEmpty()) return
        val next = index.coerceIn(0, pages.lastIndex)
        _ui.update { it.copy(pageIndex = next) }
        persist()
    }

    fun nextPage() = goToPage(_ui.value.pageIndex + 1)
    fun previousPage() = goToPage(_ui.value.pageIndex - 1)

    fun play() {
        viewModelScope.launch {
            val current = _ui.value
            val page = current.pages.getOrNull(current.pageIndex) ?: return@launch
            val book = current.book ?: return@launch
            val segments = segmenter.segment(page.text)
            if (segments.isEmpty()) {
                _ui.update { it.copy(error = "This page does not have text to read yet.") }
                return@launch
            }
            _ui.update { it.copy(loadingSpeech = true, error = null) }
            runCatching {
                playback.prepare(
                    book.id,
                    page.id,
                    page.pageNumber,
                    segments,
                    current.selectedVoice,
                    settings.get().playbackSpeed,
                )
                playback.play()
            }.onFailure {
                _ui.update { it.copy(error = "Voice playback could not be generated.") }
            }
            _ui.update { it.copy(loadingSpeech = false) }
        }
    }

    fun pause() = playback.pause()
    fun resume() = playback.play()
    fun nextSentence() = playback.next()
    fun previousSentence() = playback.previous()
    fun skipParagraph() = playback.skipParagraph()
    fun repeatSentence() = playback.repeatSentence()
    fun restartPage() = playback.restartPage()
    fun setSpeed(speed: Float) {
        playback.setSpeed(speed)
        viewModelScope.launch { settings.update { it.copy(playbackSpeed = speed) } }
    }

    fun bookmark() {
        val current = _ui.value
        val book = current.book ?: return
        viewModelScope.launch {
            bookmarks.add(Bookmark(UUID.randomUUID().toString(), book.id, current.pageIndex + 1, 0, System.currentTimeMillis()))
        }
    }

    fun updateNoteDraft(value: String) = _ui.update { it.copy(noteDraft = value) }

    fun saveNote() {
        val current = _ui.value
        val book = current.book ?: return
        val body = current.noteDraft.trim()
        if (body.isEmpty()) return
        viewModelScope.launch {
            notes.upsert(
                Note(
                    id = UUID.randomUUID().toString(),
                    bookId = book.id,
                    pageNumber = current.pageIndex + 1,
                    selectedText = "",
                    body = body,
                    startOffset = 0,
                    endOffset = 0,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                ),
            )
            _ui.update { it.copy(noteDraft = "") }
        }
    }

    private fun persist() {
        val current = _ui.value
        val book = current.book ?: return
        viewModelScope.launch {
            progress.save(
                ReadingProgress(
                    bookId = book.id,
                    pageNumber = current.pageIndex + 1,
                    segmentId = current.playback.segments.getOrNull(current.playback.currentSegmentIndex)?.id,
                    characterOffset = current.playback.segments.getOrNull(current.playback.currentSegmentIndex)?.startOffset ?: 0,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }
    }
}

fun defaultStandardVoice() = VoiceProfile(
    id = "standard-neural",
    name = "Standard voice",
    provider = "backend-tts",
    providerVoiceId = "standard",
    supportedLanguages = listOf(
        SupportedLanguage.ENGLISH,
        SupportedLanguage.HINDI,
        SupportedLanguage.TELUGU,
    ),
    status = VoiceStatus.READY,
    isCloned = false,
    qualityNote = "High-quality standard voices from the server. Custom cloned voice quality varies by language.",
    createdAt = 0,
    updatedAt = 0,
)
