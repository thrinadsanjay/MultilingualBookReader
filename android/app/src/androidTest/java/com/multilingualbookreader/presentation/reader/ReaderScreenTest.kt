package com.multilingualbookreader.presentation.reader

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.multilingualbookreader.presentation.theme.BookReaderTheme
import org.junit.Rule
import org.junit.Test

class ReaderScreenTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun showsBookChromeAndOverflow() {
        composeRule.setContent {
            BookReaderTheme(darkTheme = true, highContrast = false, fontScale = 1.25f) {
                ReaderScreen(
                    state = ReaderUiState(),
                    onBack = {},
                    onSearch = {},
                    onPlay = {},
                    onPause = {},
                    onResume = {},
                    onPrevSentence = {},
                    onNextSentence = {},
                    onSkipParagraph = {},
                    onRepeat = {},
                    onRestart = {},
                    onSpeed = {},
                    onPrevPage = {},
                    onNextPage = {},
                    onBookmark = {},
                    onNoteDraft = {},
                    onSaveNote = {},
                    onAddPages = {},
                )
            }
        }
        composeRule.onNodeWithContentDescription("Play").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Add pages").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("More options").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Add a note").assertIsDisplayed()
        composeRule.onNodeWithText("1.0x").assertIsDisplayed()
    }
}
