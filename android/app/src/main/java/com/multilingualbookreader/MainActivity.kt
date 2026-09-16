package com.multilingualbookreader

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.multilingualbookreader.domain.model.ThemeMode
import com.multilingualbookreader.domain.repository.SettingsRepository
import com.multilingualbookreader.presentation.navigation.BookReaderNavHost
import com.multilingualbookreader.presentation.theme.BookReaderTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject lateinit var settings: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appSettings by settings.observe().collectAsStateWithLifecycle(
                initialValue = com.multilingualbookreader.domain.model.AppSettings(),
            )
            val dark = when (appSettings.themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            BookReaderTheme(
                darkTheme = dark,
                highContrast = appSettings.highContrast,
                fontScale = appSettings.fontScale,
            ) {
                BookReaderNavHost()
            }
        }
    }
}
