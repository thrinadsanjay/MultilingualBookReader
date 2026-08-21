package com.multilingualbookreader.presentation.library

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.multilingualbookreader.presentation.theme.BookReaderTheme
import org.junit.Rule
import org.junit.Test

class LibraryScreenTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun emptyLibraryMessage() {
        composeRule.setContent {
            BookReaderTheme(darkTheme = false, highContrast = true, fontScale = 1.25f) {
                LibraryScreen(books = emptyList(), onOpen = {}, onBack = {}, onDelete = {})
            }
        }
        composeRule.onNodeWithText("My Library").assertIsDisplayed()
    }
}
