package com.multilingualbookreader.presentation.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.multilingualbookreader.presentation.theme.BookReaderTheme
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsPrimaryActions() {
        composeRule.setContent {
            BookReaderTheme(darkTheme = false, highContrast = false, fontScale = 1.25f) {
                HomeScreen(
                    state = HomeUiState(online = true),
                    onScan = {},
                    onImportPdf = {},
                    onLibrary = {},
                    onVoice = {},
                    onSettings = {},
                    onContinue = {},
                )
            }
        }
        composeRule.onNodeWithText("Open a book and listen.").assertIsDisplayed()
        composeRule.onNodeWithText("Scan Book").assertIsDisplayed()
    }
}
