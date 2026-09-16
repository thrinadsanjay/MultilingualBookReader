package com.multilingualbookreader.presentation.pdf

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.multilingualbookreader.presentation.theme.BookReaderTheme
import org.junit.Rule
import org.junit.Test

class PdfImportScreenTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun showsChooser() {
        composeRule.setContent {
            BookReaderTheme(darkTheme = false, highContrast = false, fontScale = 1.25f) {
                PdfImportScreen(state = PdfImportUiState(), onPick = {}, onBack = {})
            }
        }
        composeRule.onNodeWithText("Choose PDF").assertIsDisplayed()
    }
}
