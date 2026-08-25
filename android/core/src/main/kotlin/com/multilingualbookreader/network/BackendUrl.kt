package com.multilingualbookreader.network

import java.net.URI

/**
 * Points the app at a self-hosted server that is only known after the app ships.
 *
 * The Retrofit base URL is fixed at build time, so requests are rewritten onto whatever server the
 * user saved in Settings, including any path prefix such as https://example.org/svara/.
 */
object BackendUrl {
    /** Cleans up what someone types: adds a scheme, trims spaces, and guarantees a trailing slash. */
    fun normalise(input: String): String? {
        val trimmed = input.trim().trimEnd('?', '#')
        if (trimmed.isEmpty()) return null
        val withScheme = if (trimmed.contains("://")) trimmed else "https://$trimmed"
        val uri = runCatching { URI(withScheme) }.getOrNull() ?: return null
        if (uri.host.isNullOrBlank()) return null
        if (uri.scheme !in setOf("http", "https")) return null
        val path = uri.path.orEmpty().trimEnd('/')
        val port = if (uri.port == -1) "" else ":${uri.port}"
        return "${uri.scheme}://${uri.host}$port$path/"
    }

    /** Moves [requestUrl] onto [baseUrl]'s scheme, host, port, and path prefix. */
    fun rewrite(requestUrl: String, baseUrl: String): String {
        val base = runCatching { URI(normalise(baseUrl) ?: return requestUrl) }.getOrNull() ?: return requestUrl
        val request = runCatching { URI(requestUrl) }.getOrNull() ?: return requestUrl
        val prefix = base.path.orEmpty().trimEnd('/')
        val path = request.path.orEmpty().ifEmpty { "/" }
        val port = if (base.port == -1) "" else ":${base.port}"
        val query = request.query?.let { "?$it" }.orEmpty()
        return "${base.scheme}://${base.host}$port$prefix$path$query"
    }

    /** True when the value looks like something we can actually call. */
    fun isValid(input: String): Boolean = normalise(input) != null
}
