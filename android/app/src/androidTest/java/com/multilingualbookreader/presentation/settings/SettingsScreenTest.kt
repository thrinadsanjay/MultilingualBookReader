package com.multilingualbookreader.presentation.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.multilingualbookreader.domain.model.OcrRoute
import com.multilingualbookreader.domain.model.ThemeMode
import com.multilingualbookreader.presentation.theme.BookReaderTheme
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun showsAccessibilityControls() {
        composeRule.setContent {
            BookReaderTheme(darkTheme = true, highContrast = true, fontScale = 1.5f) {
                SettingsScreen(
                    theme = ThemeMode.SYSTEM,
                    fontScale = 1.25f,
                    highContrast = true,
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
                )
            }
        }
        composeRule.onNodeWithText("Settings").assertIsDisplayed()
        composeRule.onNodeWithText("High contrast").assertIsDisplayed()
    }
}
