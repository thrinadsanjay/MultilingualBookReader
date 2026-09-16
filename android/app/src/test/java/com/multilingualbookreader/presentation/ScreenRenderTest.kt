package com.multilingualbookreader.presentation

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.multilingualbookreader.camera.PageImageProcessor
import com.multilingualbookreader.domain.model.Book
import com.multilingualbookreader.domain.model.BookPage
import com.multilingualbookreader.domain.model.BookSource
import com.multilingualbookreader.domain.model.LibraryBook
import com.multilingualbookreader.domain.model.OcrRoute
import com.multilingualbookreader.domain.model.ProcessingStatus
import com.multilingualbookreader.domain.model.SupportedLanguage
import com.multilingualbookreader.domain.model.ThemeMode
import com.multilingualbookreader.presentation.components.ReaderBottomBar
import com.multilingualbookreader.presentation.home.HomeScreen
import com.multilingualbookreader.presentation.home.HomeUiState
import com.multilingualbookreader.presentation.library.LibraryScreen
import com.multilingualbookreader.presentation.scan.PageDraft
import com.multilingualbookreader.presentation.scan.ScanScreen
import com.multilingualbookreader.presentation.scan.ScanUiState
import com.multilingualbookreader.presentation.reader.ReaderScreen
import com.multilingualbookreader.presentation.reader.ReaderUiState
import com.multilingualbookreader.presentation.settings.AboutRoute
import com.multilingualbookreader.presentation.settings.ServerScreen
import com.multilingualbookreader.presentation.settings.ServerUiState
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
    fun scanGalleryChooserLight() = render("scan-gallery-light", dark = false) {
        ScanScreen(
            state = ScanUiState(),
            cameraGranted = false,
            onCapture = {},
            onPickGallery = {},
            onRequestCamera = {},
            onTextChange = {},
            onSave = {},
            onRetake = {},
            onRead = {},
            onBack = {},
        )
    }

    @Test
    fun scanGalleryChooserDark() = render("scan-gallery-dark", dark = true) {
        ScanScreen(
            state = ScanUiState(pageCount = 2),
            cameraGranted = false,
            onCapture = {},
            onPickGallery = {},
            onRequestCamera = {},
            onTextChange = {},
            onSave = {},
            onRetake = {},
            onRead = {},
            onBack = {},
        )
    }

    @Test
    fun scanCaptureChromeDark() = render("scan-capture-chrome-dark", dark = true) {
        ScanScreen(
            state = ScanUiState(showTips = true, highQuality = true, autoCrop = true),
            cameraGranted = false,
            onCapture = {},
            onPickGallery = {},
            onImportPdf = {},
            onRequestCamera = {},
            onTextChange = {},
            onSave = {},
            onRetake = {},
            onRead = {},
            onBack = {},
        )
    }

    @Test
    fun scanPreparePagesDark() = render("scan-prepare-pages-dark", dark = true) {
        val first = Bitmap.createBitmap(48, 64, Bitmap.Config.ARGB_8888).apply { eraseColor(0xFFE8D5B5.toInt()) }
        val second = Bitmap.createBitmap(48, 64, Bitmap.Config.ARGB_8888).apply { eraseColor(0xFFC9B8A0.toInt()) }
        ScanScreen(
            state = ScanUiState(
                drafts = listOf(PageDraft.from(first), PageDraft.from(second)),
                selectedDraftIndex = 0,
            ),
            cameraGranted = false,
            onCapture = {},
            onPickGallery = {},
            onRequestCamera = {},
            onTextChange = {},
            onSave = {},
            onRetake = {},
            onRead = {},
            onBack = {},
        )
    }

    @Test
    fun scanReviewFitsButtonsDark() = render("scan-review-buttons-dark", dark = true) {
        val page = Bitmap.createBitmap(48, 64, Bitmap.Config.ARGB_8888).apply { eraseColor(0xFFE8D5B5.toInt()) }
        ScanScreen(
            state = ScanUiState(
                drafts = listOf(PageDraft.from(page)),
                preview = page,
                ocrText = "Ogso (380yiogiso)",
                error = "The text still looks off. Rotate until the writing is upright, then detect again.",
            ),
            cameraGranted = false,
            onCapture = {},
            onPickGallery = {},
            onRequestCamera = {},
            onTextChange = {},
            onSave = {},
            onRetake = {},
            onRead = {},
            onBack = {},
        )
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
    fun readerWithAnUnreadablePage() = render("reader-failed-page-dark", dark = true) {
        ReaderScreenPreview(
            ReaderUiState(
                book = sampleBook.book,
                pages = listOf(
                    BookPage(
                        id = "page-1",
                        bookId = sampleBook.book.id,
                        pageNumber = 1,
                        imagePath = null,
                        text = "",
                        language = SupportedLanguage.UNKNOWN,
                        processingStatus = ProcessingStatus.FAILED,
                        errorMessage = "This scanned page could not be read.",
                    ),
                ),
            ),
        )
    }

    @Test
    fun serverSettingsDark() = render("server-dark", dark = true) {
        ServerScreen(
            state = ServerUiState(
                url = "https://books.example.org/",
                apiKey = "a-secret-key",
                result = "Connected. Telugu is ready. Installed: eng, hin, osd, tel.",
                ok = true,
            ),
            onUrl = {},
            onApiKey = {},
            onSave = {},
            onTest = {},
            onBack = {},
        )
    }

    @Test
    fun serverSettingsEmptyLight() = render("server-empty-light", dark = false) {
        ServerScreen(
            state = ServerUiState(),
            onUrl = {},
            onApiKey = {},
            onSave = {},
            onTest = {},
            onBack = {},
        )
    }

    @Test
    fun readerWithNoPages() = render("reader-no-pages-light", dark = false) {
        ReaderScreenPreview(ReaderUiState(book = sampleBook.book, pages = emptyList()))
    }

    @Test
    fun readerShowsSavedScanText() = render("reader-saved-scan-dark", dark = true) {
        ReaderScreenPreview(
            ReaderUiState(
                book = sampleBook.book.copy(title = "Scanned book", sourceType = BookSource.CAMERA_SCAN),
                pages = listOf(
                    BookPage(
                        id = "page-1",
                        bookId = sampleBook.book.id,
                        pageNumber = 1,
                        imagePath = "/pages/1.jpg",
                        text = "నమస్కారం\nThis page was saved from Scan Book.",
                        language = SupportedLanguage.TELUGU,
                        processingStatus = ProcessingStatus.COMPLETED,
                    ),
                ),
            ),
        )
    }

    @Test
    fun readerShowsOriginalScanPage() = render("reader-book-page-dark", dark = true) {
        ReaderScreenPreview(
            ReaderUiState(
                book = sampleBook.book.copy(title = "Scanned book", sourceType = BookSource.CAMERA_SCAN),
                pages = listOf(sampleScanPage(1, samplePageJpeg("యథాతథము"))),
            ),
        )
    }

    @Test
    fun readerTurnsPagesWithArrows() = render("reader-book-arrows-dark", dark = true) {
        ReaderScreenPreview(
            ReaderUiState(
                book = sampleBook.book.copy(title = "Scanned book", sourceType = BookSource.CAMERA_SCAN),
                pageIndex = 0,
                pages = listOf(
                    sampleScanPage(1, samplePageJpeg("Page one")),
                    sampleScanPage(2, samplePageJpeg("Page two")),
                ),
            ),
        )
    }

    @Composable
    private fun ReaderScreenPreview(state: ReaderUiState) {
        ReaderScreen(
            state = state,
            onBack = {},
            onSearch = {},
            onPlay = {},
            onPause = {},
            onResume = {},
            onPrevSentence = {},
            onNextSentence = {},
            onSkipParagraph = {},
            onRepeat = {},
            onRestart = {},
            onSpeed = {},
            onPrevPage = {},
            onNextPage = {},
            onBookmark = {},
            onNoteDraft = {},
            onSaveNote = {},
        )
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

    private fun sampleScanPage(number: Int, imagePath: String) = BookPage(
        id = "page-$number",
        bookId = sampleBook.book.id,
        pageNumber = number,
        imagePath = imagePath,
        text = "Page $number",
        language = SupportedLanguage.TELUGU,
        processingStatus = ProcessingStatus.COMPLETED,
    )

    private fun samplePageJpeg(heading: String): String {
        val bitmap = PageImageProcessor.newPageCanvas(720, 1024)
        val paint = Paint().apply {
            color = Color.rgb(42, 32, 22)
            textSize = 48f
            isAntiAlias = true
        }
        Canvas(bitmap).apply {
            drawText(heading, 64f, 140f, paint)
            drawText("A scanned book page.", 64f, 210f, paint)
            drawRect(64f, 280f, 656f, 860f, Paint().apply {
                color = Color.rgb(232, 214, 190)
                style = Paint.Style.FILL
            })
        }
        val file = File.createTempFile("scan-page-", ".jpg")
        file.writeBytes(PageImageProcessor.toJpeg(bitmap, 90))
        return file.absolutePath
    }
}
