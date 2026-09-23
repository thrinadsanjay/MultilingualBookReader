package com.multilingualbookreader.network

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BackendUrlTest {
    @Test
    fun assumesHttpsWhenNoSchemeIsTyped() {
        assertThat(BackendUrl.normalise("books.example.org")).isEqualTo("https://books.example.org/")
    }

    @Test
    fun keepsAnExplicitSchemeAndPort() {
        assertThat(BackendUrl.normalise("http://192.168.1.20:8080")).isEqualTo("http://192.168.1.20:8080/")
    }

    @Test
    fun tidiesSpacesAndTrailingSlashes() {
        assertThat(BackendUrl.normalise("  https://books.example.org//  ")).isEqualTo("https://books.example.org/")
    }

    @Test
    fun keepsAPathPrefixForServersBehindAReverseProxy() {
        assertThat(BackendUrl.normalise("https://example.org/svara")).isEqualTo("https://example.org/svara/")
    }

    @Test
    fun rejectsNonsense() {
        assertThat(BackendUrl.normalise("")).isNull()
        assertThat(BackendUrl.normalise("   ")).isNull()
        assertThat(BackendUrl.normalise("ftp://example.org")).isNull()
        assertThat(BackendUrl.isValid("not a url at all")).isFalse()
    }

    @Test
    fun movesARequestOntoTheConfiguredServer() {
        val rewritten = BackendUrl.rewrite(
            requestUrl = "https://api.example.com/api/v1/ocr",
            baseUrl = "https://books.example.org",
        )
        assertThat(rewritten).isEqualTo("https://books.example.org/api/v1/ocr")
    }

    @Test
    fun keepsThePortAndPathPrefixOfTheConfiguredServer() {
        val rewritten = BackendUrl.rewrite(
            requestUrl = "https://api.example.com/api/v1/ocr",
            baseUrl = "http://192.168.1.20:8080/svara",
        )
        assertThat(rewritten).isEqualTo("http://192.168.1.20:8080/svara/api/v1/ocr")
    }

    @Test
    fun preservesQueryParameters() {
        val rewritten = BackendUrl.rewrite(
            requestUrl = "https://api.example.com/api/v1/audio/x?format=mp3",
            baseUrl = "https://books.example.org",
        )
        assertThat(rewritten).isEqualTo("https://books.example.org/api/v1/audio/x?format=mp3")
    }

    @Test
    fun leavesTheRequestAloneWhenTheServerIsNotConfigured() {
        val original = "https://api.example.com/api/v1/ocr"
        assertThat(BackendUrl.rewrite(original, "")).isEqualTo(original)
        assertThat(BackendUrl.rewrite(original, "   ")).isEqualTo(original)
    }

    @Test
    fun packedUrlIgnoresEmulatorAndPlaceholderHosts() {
        assertThat(BackendUrl.isPacked("https://svara.example.org")).isTrue()
        assertThat(BackendUrl.isPacked("https://api.example.com/")).isFalse()
        assertThat(BackendUrl.isPacked("http://10.0.2.2:8080/")).isFalse()
        assertThat(BackendUrl.isPacked("")).isFalse()
    }

    @Test
    fun packedApiKeyYieldsToASavedOverride() {
        assertThat(BackendUrl.resolveApiKey("typed", "baked")).isEqualTo("typed")
        assertThat(BackendUrl.resolveApiKey("  ", "baked")).isEqualTo("baked")
        assertThat(BackendUrl.resolveApiKey(null, "")).isNull()
    }
}
