package com.multilingualbookreader.presentation.voice

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.multilingualbookreader.presentation.theme.BookReaderTheme
import org.junit.Rule
import org.junit.Test

class VoiceTestScreenUiTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun showsThreeLanguages() {
        composeRule.setContent {
            BookReaderTheme(darkTheme = false, highContrast = false, fontScale = 1.25f) {
                VoiceTestScreen(
                    message = "Test",
                    english = "Welcome to my book reader.",
                    hindi = "यह मेरी किताब है।",
                    telugu = "ఇది నా పుస్తకం.",
                    onEnglish = {},
                    onHindi = {},
                    onTelugu = {},
                    onPlay = { _, _ -> },
                    onBack = {},
                )
            }
        }
        composeRule.onNodeWithText("Play English").assertIsDisplayed()
        composeRule.onNodeWithText("Play Hindi").assertIsDisplayed()
        composeRule.onNodeWithText("Play Telugu").assertIsDisplayed()
        composeRule.onNodeWithText("ఇది నా పుస్తకం.").assertIsDisplayed()
    }
}
