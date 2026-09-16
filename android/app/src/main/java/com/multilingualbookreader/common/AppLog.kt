package com.multilingualbookreader.common

import android.util.Log
import com.multilingualbookreader.BuildConfig

/**
 * Structured logger that never accepts book contents, voice bytes, tokens, or keys.
 */
object AppLog {
    private const val TAG = "Svara"

    fun d(event: String, extras: Map<String, Any?> = emptyMap()) {
        if (BuildConfig.ENABLE_VERBOSE_LOGS) {
            Log.d(TAG, format(event, extras))
        }
    }

    fun i(event: String, extras: Map<String, Any?> = emptyMap()) {
        Log.i(TAG, format(event, extras))
    }

    fun w(event: String, extras: Map<String, Any?> = emptyMap(), throwable: Throwable? = null) {
        Log.w(TAG, format(event, extras), throwable)
    }

    fun e(event: String, extras: Map<String, Any?> = emptyMap(), throwable: Throwable? = null) {
        Log.e(TAG, format(event, extras), throwable)
    }

    private fun format(event: String, extras: Map<String, Any?>): String {
        if (extras.isEmpty()) return event
        val safe = extras.entries.joinToString(" ") { "${it.key}=${it.value}" }
        return "$event $safe"
    }
}
