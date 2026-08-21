package com.multilingualbookreader.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.multilingualbookreader.domain.model.AppSettings
import com.multilingualbookreader.domain.model.OcrRoute
import com.multilingualbookreader.domain.model.ThemeMode
import com.multilingualbookreader.domain.repository.SettingsRepository
import com.multilingualbookreader.network.BookReaderApi
import com.multilingualbookreader.network.EncryptedTokenStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val api: BookReaderApi,
    private val tokens: EncryptedTokenStore,
) : ViewModel() {
    val state = settings.observe().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())
    var authMessage: String? = null
        private set

    fun setTheme(mode: ThemeMode) = update { it.copy(themeMode = mode) }
    fun setFont(scale: Float) = update { it.copy(fontScale = scale) }
    fun setContrast(enabled: Boolean) = update { it.copy(highContrast = enabled) }
    fun setMotion(enabled: Boolean) = update { it.copy(reduceMotion = enabled) }
    fun setOcr(route: OcrRoute) = update { it.copy(ocrRoute = route) }
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

    private fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { settings.update(transform) }
    }
}
