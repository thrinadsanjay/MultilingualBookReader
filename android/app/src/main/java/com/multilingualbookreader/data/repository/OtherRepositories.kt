package com.multilingualbookreader.data.repository

import com.multilingualbookreader.database.AudioSegmentDao
import com.multilingualbookreader.database.AudioSegmentEntity
import com.multilingualbookreader.database.BookmarkDao
import com.multilingualbookreader.database.NoteDao
import com.multilingualbookreader.database.ProgressDao
import com.multilingualbookreader.database.VoiceProfileDao
import com.multilingualbookreader.database.toDomain
import com.multilingualbookreader.database.toEntity
import com.multilingualbookreader.domain.model.AppSettings
import com.multilingualbookreader.domain.model.AudioSegment
import com.multilingualbookreader.domain.model.Bookmark
import com.multilingualbookreader.domain.model.LibraryBook
import com.multilingualbookreader.domain.model.Note
import com.multilingualbookreader.domain.model.OcrRoute
import com.multilingualbookreader.domain.model.ReadingProgress
import com.multilingualbookreader.domain.model.ThemeMode
import com.multilingualbookreader.domain.model.VoiceProfile
import com.multilingualbookreader.domain.repository.AudioCacheRepository
import com.multilingualbookreader.domain.repository.BookmarkRepository
import com.multilingualbookreader.domain.repository.NoteRepository
import com.multilingualbookreader.domain.repository.ProgressRepository
import com.multilingualbookreader.domain.repository.SettingsRepository
import com.multilingualbookreader.domain.repository.VoiceRepository
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Singleton
class ProgressRepositoryImpl @Inject constructor(
    private val progressDao: ProgressDao,
    private val books: BookRepositoryImpl,
) : ProgressRepository {
    override fun observe(bookId: String) = progressDao.observe(bookId).map { it?.toDomain() }
    override suspend fun get(bookId: String) = progressDao.get(bookId)?.toDomain()
    override suspend fun save(progress: ReadingProgress) = progressDao.upsert(progress.toEntity())
    override suspend fun continueReading(): LibraryBook? {
        val latest = progressDao.latest() ?: return null
        val library = books.observeLibrary().first()
        return library.firstOrNull { it.book.id == latest.bookId }
    }
}

@Singleton
class BookmarkRepositoryImpl @Inject constructor(
    private val dao: BookmarkDao,
) : BookmarkRepository {
    override fun observe(bookId: String) = dao.observe(bookId).map { it.map(com.multilingualbookreader.database.BookmarkEntity::toDomain) }
    override suspend fun add(bookmark: Bookmark) = dao.upsert(bookmark.toEntity())
    override suspend fun remove(id: String) = dao.delete(id)
}

@Singleton
class NoteRepositoryImpl @Inject constructor(
    private val dao: NoteDao,
) : NoteRepository {
    override fun observe(bookId: String) = dao.observe(bookId).map { it.map(com.multilingualbookreader.database.NoteEntity::toDomain) }
    override suspend fun upsert(note: Note) = dao.upsert(note.toEntity())
    override suspend fun remove(id: String) = dao.delete(id)
}

@Singleton
class VoiceRepositoryImpl @Inject constructor(
    private val dao: VoiceProfileDao,
) : VoiceRepository {
    override fun observeProfiles(): Flow<List<VoiceProfile>> = dao.observeAll().map { it.map { entity -> entity.toDomain() } }
    override suspend fun get(id: String) = dao.get(id)?.toDomain()
    override suspend fun upsert(profile: VoiceProfile) = dao.upsert(profile.toEntity())
    override suspend fun delete(id: String) = dao.delete(id)
}

@Singleton
class AudioCacheRepositoryImpl @Inject constructor(
    private val dao: AudioSegmentDao,
) : AudioCacheRepository {
    override suspend fun get(cacheKey: String): AudioSegment? = dao.getByCacheKey(cacheKey)?.toDomain()
    override suspend fun put(segment: AudioSegment) {
        dao.upsert(
            AudioSegmentEntity(
                id = segment.id,
                bookPageId = segment.bookPageId,
                sequence = segment.sequence,
                textHash = segment.cacheKey.take(16),
                language = segment.language.bcp47,
                audioPath = segment.audioPath,
                durationMs = segment.durationMs,
                voiceProfileId = segment.voiceProfileId,
                status = segment.status.name,
                cacheKey = segment.cacheKey,
            ),
        )
    }
    override suspend fun deleteForVoice(voiceId: String) = dao.deleteForVoice(voiceId)
    override suspend fun deleteForBook(bookId: String) = dao.deleteForBook(bookId)
}

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {
    override fun observe(): Flow<AppSettings> = dataStore.data.map { it.toSettings() }
    override suspend fun get(): AppSettings = dataStore.data.first().toSettings()
    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        val current = get()
        val next = transform(current)
        dataStore.edit { prefs ->
            prefs[Keys.THEME] = next.themeMode.name
            prefs[Keys.FONT] = next.fontScale
            prefs[Keys.CONTRAST] = next.highContrast
            prefs[Keys.MOTION] = next.reduceMotion
            prefs[Keys.SPEED] = next.playbackSpeed
            prefs[Keys.VOICE] = next.selectedVoiceId.orEmpty()
            prefs[Keys.OCR] = next.ocrRoute.name
            prefs[Keys.LARGE] = next.largeControls
            prefs[Keys.ANALYTICS] = next.analyticsEnabled
            prefs[Keys.CRASH] = next.crashReportingEnabled
            prefs[Keys.BACKEND] = next.backendBaseUrl
        }
    }

    private fun Preferences.toSettings() = AppSettings(
        themeMode = ThemeMode.valueOf(this[Keys.THEME] ?: ThemeMode.SYSTEM.name),
        fontScale = this[Keys.FONT] ?: 1.0f,
        highContrast = this[Keys.CONTRAST] ?: false,
        reduceMotion = this[Keys.MOTION] ?: true,
        playbackSpeed = this[Keys.SPEED] ?: 1.0f,
        selectedVoiceId = this[Keys.VOICE]?.ifBlank { null },
        ocrRoute = OcrRoute.valueOf(this[Keys.OCR] ?: OcrRoute.AUTO.name),
        largeControls = this[Keys.LARGE] ?: true,
        analyticsEnabled = this[Keys.ANALYTICS] ?: false,
        crashReportingEnabled = this[Keys.CRASH] ?: false,
        backendBaseUrl = this[Keys.BACKEND] ?: "",
    )

    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val FONT = floatPreferencesKey("font")
        val CONTRAST = booleanPreferencesKey("contrast")
        val MOTION = booleanPreferencesKey("motion")
        val SPEED = floatPreferencesKey("speed")
        val VOICE = stringPreferencesKey("voice")
        val OCR = stringPreferencesKey("ocr")
        val LARGE = booleanPreferencesKey("large")
        val ANALYTICS = booleanPreferencesKey("analytics")
        val CRASH = booleanPreferencesKey("crash")
        val BACKEND = stringPreferencesKey("backend")
    }
}
