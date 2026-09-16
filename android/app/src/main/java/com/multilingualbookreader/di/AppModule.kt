package com.multilingualbookreader.di

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.multilingualbookreader.BuildConfig
import com.multilingualbookreader.database.BookReaderDatabase
import com.multilingualbookreader.domain.engine.LanguageDetector
import com.multilingualbookreader.domain.engine.OcrEngine
import com.multilingualbookreader.domain.engine.TextProcessor
import com.multilingualbookreader.domain.engine.TextToSpeechEngine
import com.multilingualbookreader.domain.engine.VoiceCloningEngine
import com.multilingualbookreader.domain.repository.AudioCacheRepository
import com.multilingualbookreader.domain.repository.BookRepository
import com.multilingualbookreader.domain.repository.BookmarkRepository
import com.multilingualbookreader.domain.repository.NoteRepository
import com.multilingualbookreader.domain.repository.ProgressRepository
import com.multilingualbookreader.domain.repository.SettingsRepository
import com.multilingualbookreader.domain.repository.VoiceRepository
import com.multilingualbookreader.domain.usecase.GetContinueReadingUseCase
import com.multilingualbookreader.domain.usecase.ObserveLibraryUseCase
import com.multilingualbookreader.domain.usecase.ObserveSettingsUseCase
import com.multilingualbookreader.domain.usecase.SaveReadingProgressUseCase
import com.multilingualbookreader.domain.usecase.SearchBookUseCase
import com.multilingualbookreader.language.ScriptLanguageDetector
import com.multilingualbookreader.network.AuthInterceptor
import com.multilingualbookreader.network.BookReaderApi
import com.multilingualbookreader.network.PlainHttp
import com.multilingualbookreader.update.GitHubReleaseApi
import com.multilingualbookreader.ocr.CompositeOcrEngine
import com.multilingualbookreader.text.DefaultTextProcessor
import com.multilingualbookreader.text.SentenceSegmenter
import com.multilingualbookreader.tts.CompositeTtsEngine
import com.multilingualbookreader.voice.BackendVoiceCloningEngine
import com.multilingualbookreader.data.repository.AudioCacheRepositoryImpl
import com.multilingualbookreader.data.repository.BookRepositoryImpl
import com.multilingualbookreader.data.repository.BookmarkRepositoryImpl
import com.multilingualbookreader.data.repository.NoteRepositoryImpl
import com.multilingualbookreader.data.repository.ProgressRepositoryImpl
import com.multilingualbookreader.data.repository.SettingsRepositoryImpl
import com.multilingualbookreader.data.repository.VoiceRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

private val Context.dataStore by preferencesDataStore("book_reader_settings")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): BookReaderDatabase =
        Room.databaseBuilder(context, BookReaderDatabase::class.java, "book-reader.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun bookDao(db: BookReaderDatabase) = db.bookDao()
    @Provides fun pageDao(db: BookReaderDatabase) = db.pageDao()
    @Provides fun textBlockDao(db: BookReaderDatabase) = db.textBlockDao()
    @Provides fun progressDao(db: BookReaderDatabase) = db.progressDao()
    @Provides fun audioDao(db: BookReaderDatabase) = db.audioDao()
    @Provides fun voiceDao(db: BookReaderDatabase) = db.voiceDao()
    @Provides fun bookmarkDao(db: BookReaderDatabase) = db.bookmarkDao()
    @Provides fun noteDao(db: BookReaderDatabase) = db.noteDao()

    @Provides
    @Singleton
    fun dataStore(@ApplicationContext context: Context): DataStore<Preferences> = context.dataStore

    @Provides
    @Singleton
    fun encryptedPrefs(@ApplicationContext context: Context): SharedPreferences {
        val master = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        return EncryptedSharedPreferences.create(
            context,
            "secure_tokens",
            master,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    @Provides
    @Singleton
    fun json(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    @PlainHttp
    fun plainOkHttp(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("User-Agent", "MultilingualBookReader/${BuildConfig.VERSION_NAME}")
                        .build(),
                )
            }
            .callTimeout(180, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun okHttp(auth: AuthInterceptor, @PlainHttp plain: OkHttpClient): OkHttpClient {
        return plain.newBuilder()
            .addInterceptor(auth)
            .build()
    }

    @Provides
    @Singleton
    fun gitHubApi(@PlainHttp client: OkHttpClient, json: Json): GitHubReleaseApi {
        val contentType = "application/json".toMediaType()
        val githubClient = client.newBuilder()
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("Accept", "application/vnd.github+json")
                        .build(),
                )
            }
            .build()
        return Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(githubClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(GitHubReleaseApi::class.java)
    }

    @Provides
    @Singleton
    fun api(client: OkHttpClient, json: Json): BookReaderApi {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(BookReaderApi::class.java)
    }

    @Provides
    @Singleton
    fun languageDetector(): LanguageDetector = ScriptLanguageDetector()

    @Provides
    @Singleton
    fun textProcessor(): TextProcessor = DefaultTextProcessor()

    @Provides
    @Singleton
    fun sentenceSegmenter(detector: LanguageDetector): SentenceSegmenter = SentenceSegmenter(detector)

    @Provides
    fun observeLibrary(repo: BookRepository) = ObserveLibraryUseCase(repo)

    @Provides
    fun continueReading(repo: ProgressRepository) = GetContinueReadingUseCase(repo)

    @Provides
    fun saveProgress(repo: ProgressRepository) = SaveReadingProgressUseCase(repo)

    @Provides
    fun search(repo: BookRepository) = SearchBookUseCase(repo)

    @Provides
    fun observeSettings(repo: SettingsRepository) = ObserveSettingsUseCase(repo)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class BindingsModule {
    @Binds @Singleton abstract fun books(impl: BookRepositoryImpl): BookRepository
    @Binds @Singleton abstract fun progress(impl: ProgressRepositoryImpl): ProgressRepository
    @Binds @Singleton abstract fun bookmarks(impl: BookmarkRepositoryImpl): BookmarkRepository
    @Binds @Singleton abstract fun notes(impl: NoteRepositoryImpl): NoteRepository
    @Binds @Singleton abstract fun voices(impl: VoiceRepositoryImpl): VoiceRepository
    @Binds @Singleton abstract fun settings(impl: SettingsRepositoryImpl): SettingsRepository
    @Binds @Singleton abstract fun audio(impl: AudioCacheRepositoryImpl): AudioCacheRepository
    @Binds @Singleton abstract fun ocr(impl: CompositeOcrEngine): OcrEngine
    @Binds @Singleton abstract fun tts(impl: CompositeTtsEngine): TextToSpeechEngine
    @Binds @Singleton abstract fun voice(impl: BackendVoiceCloningEngine): VoiceCloningEngine
}
