package com.multilingualbookreader.network

import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Sends every backend call to the server the user configured, with whatever credential they gave.
 *
 * The Retrofit base URL is baked in at build time, but a self-hosted server is only known later,
 * so the request is re-pointed here instead.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val prefs: EncryptedTokenStore,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val builder = original.newBuilder()

        val server = prefs.serverUrl()
        val request = if (server.isNullOrBlank()) {
            builder
        } else {
            builder.url(BackendUrl.rewrite(original.url.toString(), server))
        }

        prefs.apiKey()?.takeIf { it.isNotBlank() }?.let { request.header("X-API-Key", it) }
        prefs.accessToken()?.takeIf { it.isNotBlank() }?.let { request.header("Authorization", "Bearer $it") }
        return chain.proceed(request.build())
    }
}

/**
 * Encrypted storage for the backend address and its credentials. Kept in SharedPreferences rather
 * than DataStore so the network interceptor can read it without blocking on a coroutine.
 */
@Singleton
class EncryptedTokenStore @Inject constructor(
    private val prefs: SharedPreferences,
) {
    fun accessToken(): String? = prefs.getString(KEY_ACCESS, null)
    fun save(access: String, refresh: String) {
        prefs.edit().putString(KEY_ACCESS, access).putString(KEY_REFRESH, refresh).apply()
    }

    fun serverUrl(): String? = prefs.getString(KEY_SERVER_URL, null)
    fun apiKey(): String? = prefs.getString(KEY_API_KEY, null)

    /** Stores a normalised URL, or clears it when the field is emptied. */
    fun saveServer(url: String, apiKey: String) {
        val normalised = BackendUrl.normalise(url)
        prefs.edit()
            .apply { if (normalised == null) remove(KEY_SERVER_URL) else putString(KEY_SERVER_URL, normalised) }
            .apply { if (apiKey.isBlank()) remove(KEY_API_KEY) else putString(KEY_API_KEY, apiKey.trim()) }
            .apply()
    }

    fun clear() {
        prefs.edit().remove(KEY_ACCESS).remove(KEY_REFRESH).apply()
    }

    private companion object {
        const val KEY_ACCESS = "access_token"
        const val KEY_REFRESH = "refresh_token"
        const val KEY_SERVER_URL = "server_url"
        const val KEY_API_KEY = "server_api_key"
    }
}
