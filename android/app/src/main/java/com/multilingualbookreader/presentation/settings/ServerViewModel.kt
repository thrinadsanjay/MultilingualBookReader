package com.multilingualbookreader.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.multilingualbookreader.domain.repository.SettingsRepository
import com.multilingualbookreader.network.BackendUrl
import com.multilingualbookreader.network.BookReaderApi
import com.multilingualbookreader.network.EncryptedTokenStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ServerUiState(
    val url: String = "",
    val apiKey: String = "",
    val saved: Boolean = false,
    val testing: Boolean = false,
    val result: String? = null,
    val ok: Boolean = false,
) {
    /** Android blocks unencrypted traffic, and a key sent over http would be readable in transit. */
    val insecureWarning: String?
        get() {
            val normalised = BackendUrl.normalise(url) ?: return null
            val localhost = listOf("localhost", "127.0.0.1", "10.0.2.2").any { normalised.contains("://$it") }
            return if (normalised.startsWith("http://") && !localhost) {
                "Use https so your key is not sent in the clear. Android also blocks plain http."
            } else {
                null
            }
        }
}

@HiltViewModel
class ServerViewModel @Inject constructor(
    private val credentials: EncryptedTokenStore,
    private val api: BookReaderApi,
    private val settings: SettingsRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(
        ServerUiState(url = credentials.serverUrl().orEmpty(), apiKey = credentials.apiKey().orEmpty()),
    )
    val state: StateFlow<ServerUiState> = _state

    fun setUrl(value: String) {
        _state.value = _state.value.copy(url = value, saved = false, result = null)
    }

    fun setApiKey(value: String) {
        _state.value = _state.value.copy(apiKey = value, saved = false, result = null)
    }

    fun save() {
        val current = _state.value
        if (current.url.isNotBlank() && !BackendUrl.isValid(current.url)) {
            _state.value = current.copy(result = "That does not look like a web address.", ok = false)
            return
        }
        credentials.saveServer(current.url, current.apiKey)
        val normalised = BackendUrl.normalise(current.url).orEmpty()
        viewModelScope.launch { settings.update { it.copy(backendBaseUrl = normalised) } }
        _state.value = current.copy(url = normalised, saved = true, result = null)
    }

    /** Saves first, then calls the server so the answer reflects what the app will actually use. */
    fun test() {
        save()
        if (_state.value.result != null) return
        _state.value = _state.value.copy(testing = true, result = null)
        viewModelScope.launch {
            runCatching { api.ocrHealth() }
                .onSuccess { health ->
                    val languages = health.languages.joinToString(", ").ifBlank { "none reported" }
                    _state.value = _state.value.copy(
                        testing = false,
                        ok = health.teluguReady,
                        result = if (health.teluguReady) {
                            "Connected. Telugu is ready. Installed: $languages."
                        } else {
                            "Connected, but Telugu is missing. Install tesseract-ocr-tel and restart. Installed: $languages."
                        },
                    )
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        testing = false,
                        ok = false,
                        result = "Could not reach the server: ${error.message?.take(120) ?: "unknown error"}",
                    )
                }
        }
    }
}
