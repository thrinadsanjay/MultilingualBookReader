package com.multilingualbookreader.presentation

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.multilingualbookreader.domain.model.Book
import com.multilingualbookreader.domain.model.BookSource
import com.multilingualbookreader.domain.model.LibraryBook
import com.multilingualbookreader.domain.model.OcrRoute
import com.multilingualbookreader.domain.model.SupportedLanguage
import com.multilingualbookreader.domain.model.ThemeMode
import com.multilingualbookreader.presentation.components.ReaderBottomBar
import com.multilingualbookreader.presentation.home.HomeScreen
import com.multilingualbookreader.presentation.home.HomeUiState
import com.multilingualbookreader.presentation.library.LibraryScreen
import com.multilingualbookreader.presentation.settings.AboutRoute
import com.multilingualbookreader.presentation.settings.SettingsScreen
import com.multilingualbookreader.presentation.settings.UpdatesScreen
import com.multilingualbookreader.presentation.theme.BookReaderTheme
import com.multilingualbookreader.presentation.voice.VoiceScreen
import com.multilingualbookreader.presentation.voice.VoiceUiState
import com.multilingualbookreader.update.AvailableUpdate
import com.multilingualbookreader.update.InstallChannel
import com.multilingualbookreader.update.UpdateMessages
import com.multilingualbookreader.update.UpdatePhase
import com.multilingualbookreader.update.UpdateStage
import com.multilingualbookreader.update.UpdateUiState
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders every tab in both themes. Robolectric's native graphics mode draws real pixels, so this
 * catches layout crashes and lets the PNGs in build/screenshots be reviewed against the design.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class ScreenRenderTest {
    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val pendingUpdate = AvailableUpdate(
        versionCode = 9,
        versionName = "v0.1.8",
        apkUrl = "https://example.test/build",
        apkName = "BookReader-9-debug.apk",
        notes = "versionCode=9\nImproved OCR\nImproved voice processing\nUI improvements",
    )

    private val updatePhases = listOf(
        "idle" to UpdatePhase.Idle,
        "checking" to UpdatePhase.Checking,
        "uptodate" to UpdatePhase.UpToDate,
        "available" to UpdatePhase.Available(pendingUpdate),
        "downloading" to UpdatePhase.Downloading(pendingUpdate, 45),
        "downloaded" to UpdatePhase.Downloaded(pendingUpdate),
        "installing" to UpdatePhase.Installing(pendingUpdate),
        "offline" to UpdatePhase.Failed(UpdateStage.CHECK, null, UpdateMessages.OFFLINE),
        "download-failed" to UpdatePhase.Failed(UpdateStage.DOWNLOAD, pendingUpdate, UpdateMessages.DOWNLOAD_FAILED),
        "install-failed" to UpdatePhase.Failed(UpdateStage.INSTALL, pendingUpdate, UpdateMessages.INSTALL_FAILED),
    )

    private val sampleBook = LibraryBook(
        book = Book(
            id = "book-1",
            title = "My Sample Book",
            author = "Anon",
            coverPath = null,
            sourceType = BookSource.MIXED,
            language = SupportedLanguage.ENGLISH,
            totalPages = 120,
            createdAt = 0L,
            updatedAt = 0L,
        ),
        completedPages = 24,
        progressPercent = 20,
        lastReadAt = 0L,
    )

    @Test
    fun homeDark() = render("home-dark", dark = true) {
        HomeScreen(
            state = HomeUiState(continueReading = sampleBook, online = true),
            onScan = {},
            onImportPdf = {},
            onLibrary = {},
            onVoice = {},
            onContinue = {},
        )
    }

    @Test
    fun homeLight() = render("home-light", dark = false) {
        HomeScreen(
            state = HomeUiState(continueReading = sampleBook, online = true),
            onScan = {},
            onImportPdf = {},
            onLibrary = {},
            onVoice = {},
            onContinue = {},
        )
    }

    @Test
    fun homeWithNavigationDark() = render("home-nav-dark", dark = true) {
        Scaffold(bottomBar = { ReaderBottomBar(currentRoute = "home") {} }) { padding ->
            Box(Modifier.padding(padding)) {
                HomeScreen(
                    state = HomeUiState(continueReading = sampleBook, online = true),
                    onScan = {},
                    onImportPdf = {},
                    onLibrary = {},
                    onVoice = {},
                            onContinue = {},
                )
            }
        }
    }

    @Test
    fun homeWithNavigationLight() = render("home-nav-light", dark = false) {
        Scaffold(bottomBar = { ReaderBottomBar(currentRoute = "home") {} }) { padding ->
            Box(Modifier.padding(padding)) {
                HomeScreen(
                    state = HomeUiState(continueReading = sampleBook, online = true),
                    onScan = {},
                    onImportPdf = {},
                    onLibrary = {},
                    onVoice = {},
                            onContinue = {},
                )
            }
        }
    }

    @Test
    fun libraryDark() = render("library-dark", dark = true) {
        LibraryScreen(books = listOf(sampleBook), onOpen = {}, onBack = {}, onDelete = {})
    }

    @Test
    fun libraryEmptyLight() = render("library-empty-light", dark = false) {
        LibraryScreen(books = emptyList(), onOpen = {}, onBack = {}, onDelete = {})
    }

    @Test
    fun voiceDark() = render("voice-dark", dark = true) {
        VoiceScreen(
            state = VoiceUiState(),
            profiles = emptyList(),
            micGranted = true,
            onRequestMic = {},
            onName = {},
            onConsent = {},
            onStart = {},
            onStop = {},
            onCreate = {},
            onDelete = {},
            onSelect = {},
            onTest = {},
            onBack = {},
            showBack = false,
        )
    }

    @Test
    fun aboutDark() = render("about-dark", dark = true) {
        AboutRoute(onBack = {})
    }

    @Test
    fun settingsLight() = render("settings-light", dark = false) {
        SettingsScreen(
            theme = ThemeMode.LIGHT,
            fontScale = 1.0f,
            highContrast = false,
            reduceMotion = true,
            ocrRoute = OcrRoute.AUTO,
            analytics = false,
            crash = false,
            onTheme = {},
            onFont = {},
            onContrast = {},
            onMotion = {},
            onOcr = {},
            onAnalytics = {},
            onCrash = {},
            onPrivacy = {},
            onVoiceTest = {},
            onBack = {},
            showBack = false,
        )
    }

    @Test
    fun settingsDark() = render("settings-dark", dark = true) {
        SettingsScreen(
            theme = ThemeMode.DARK,
            fontScale = 1.0f,
            highContrast = false,
            reduceMotion = true,
            ocrRoute = OcrRoute.AUTO,
            analytics = false,
            crash = false,
            onTheme = {},
            onFont = {},
            onContrast = {},
            onMotion = {},
            onOcr = {},
            onAnalytics = {},
            onCrash = {},
            onPrivacy = {},
            onVoiceTest = {},
            onBack = {},
            showBack = false,
        )
    }

    @Test
    fun updateStatesDark() = renderEveryUpdatePhase(dark = true, suffix = "dark")

    @Test
    fun updateStatesLight() = renderEveryUpdatePhase(dark = false, suffix = "light")

    /**
     * Walks the whole state machine inside one composition, because a test may only call
     * setContent once. The clock is stepped manually so the busy-state spinners cannot stall
     * waitForIdle.
     */
    private fun renderEveryUpdatePhase(dark: Boolean, suffix: String) {
        val phase = mutableStateOf<UpdatePhase>(UpdatePhase.Idle)
        composeRule.setContent {
            BookReaderTheme(darkTheme = dark, highContrast = false, fontScale = 1.0f) {
                UpdatesScreen(
                    updateState = UpdateUiState(installedVersionName = "0.1.7", installedVersionCode = 8, phase = phase.value),
                    channel = InstallChannel.DIRECT,
                    onAction = {},
                    onBack = {},
                )
            }
        }
        composeRule.mainClock.autoAdvance = false
        updatePhases.forEach { (name, next) ->
            phase.value = next
            composeRule.mainClock.advanceTimeByFrame()
            composeRule.mainClock.advanceTimeByFrame()
            capture("updates-$name-$suffix")
        }
    }

    @Test
    fun updatesBlockedByAdvancedProtection() {
        renderUpdate("updates-blocked-dark", dark = true, phase = UpdatePhase.UpToDate, channel = InstallChannel.BLOCKED)
    }

    @Test
    fun updatesFromPlay() {
        renderUpdate("updates-play-light", dark = false, phase = UpdatePhase.Idle, channel = InstallChannel.PLAY)
    }

    private fun renderUpdate(
        name: String,
        dark: Boolean,
        phase: UpdatePhase,
        channel: InstallChannel = InstallChannel.DIRECT,
    ) = render(name, dark) {
        UpdatesScreen(
            updateState = UpdateUiState(installedVersionName = "0.1.7", installedVersionCode = 8, phase = phase),
            channel = channel,
            onAction = {},
            onBack = {},
        )
    }

    private fun render(name: String, dark: Boolean, content: @Composable () -> Unit) {
        composeRule.setContent {
            BookReaderTheme(darkTheme = dark, highContrast = false, fontScale = 1.0f) { content() }
        }
        composeRule.waitForIdle()
        capture(name)
    }

    private fun capture(name: String) {
        val view = composeRule.activity.window.decorView
        val width = view.width.takeIf { it > 0 } ?: 1080
        val height = view.height.takeIf { it > 0 } ?: 2340
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        val dir = File("build/screenshots").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
