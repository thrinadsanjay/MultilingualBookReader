package com.multilingualbookreader.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.multilingualbookreader.domain.model.AppSettings
import com.multilingualbookreader.domain.model.OcrRoute
import com.multilingualbookreader.domain.model.ThemeMode
import com.multilingualbookreader.domain.repository.BookRepository
import com.multilingualbookreader.domain.repository.SettingsRepository
import com.multilingualbookreader.domain.repository.VoiceRepository
import com.multilingualbookreader.network.BookReaderApi
import com.multilingualbookreader.network.EncryptedTokenStore
import com.multilingualbookreader.update.AppUpdateManager
import com.multilingualbookreader.update.UpdateActionKind
import com.multilingualbookreader.update.UpdatePhase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val books: BookRepository,
    private val voices: VoiceRepository,
    private val api: BookReaderApi,
    private val tokens: EncryptedTokenStore,
    val updates: AppUpdateManager,
) : ViewModel() {
    val state = settings.observe().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())
    val updateState = updates.state
    private var downloadJob: Job? = null

    fun setTheme(mode: ThemeMode) = update { it.copy(themeMode = mode) }
    fun setFont(scale: Float) = update { it.copy(fontScale = scale) }
    fun setContrast(enabled: Boolean) = update { it.copy(highContrast = enabled) }
    fun setMotion(enabled: Boolean) = update { it.copy(reduceMotion = enabled) }
    fun setOcr(route: OcrRoute) = update { it.copy(ocrRoute = route) }
    fun setLanguage(tag: String) = update { it.copy(defaultLanguageTag = tag) }
    fun setBackend(url: String) = update { it.copy(backendBaseUrl = url) }
    fun setAnalytics(enabled: Boolean) = update { it.copy(analyticsEnabled = enabled) }
    fun setCrash(enabled: Boolean) = update { it.copy(crashReportingEnabled = enabled) }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            runCatching {
                val result = api.login(com.multilingualbookreader.network.AuthRequest(email, password))
                tokens.save(result.accessToken, result.refreshToken)
            }
        }
    }

    fun signOut() = tokens.clear()

    fun installChannel() = updates.installChannel()

    /** Every update button routes here, so the phase always drives what actually happens. */
    fun onUpdateAction(kind: UpdateActionKind) {
        when (kind) {
            UpdateActionKind.CHECK -> {
                downloadJob?.cancel()
                downloadJob = viewModelScope.launch { updates.check() }
            }
            UpdateActionKind.DOWNLOAD -> {
                downloadJob?.cancel()
                downloadJob = viewModelScope.launch { updates.download() }
            }
            UpdateActionKind.INSTALL -> updates.startInstall()
            UpdateActionKind.OPEN_PLAY -> updates.openPlayStore()
            UpdateActionKind.GRANT_PERMISSION -> updates.openInstallPermissionSettings()
            UpdateActionKind.CANCEL -> {
                downloadJob?.cancel()
                downloadJob = null
                if (updates.state.value.phase is UpdatePhase.Failed) updates.dismissFailure() else updates.cancelDownload()
            }
            UpdateActionKind.BUSY -> Unit
        }
    }

    /** Called when the screen resumes so a cancelled system install does not stay "Installing…". */
    fun onUpdateScreenResumed() = updates.installerReturned()

    fun deleteAllData() {
        viewModelScope.launch {
            books.observeLibrary().first().forEach { books.deleteBook(it.book.id) }
            voices.observeProfiles().first().forEach { voices.delete(it.id) }
            tokens.clear()
            settings.update { AppSettings() }
        }
    }

    private fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { settings.update(transform) }
    }
}
