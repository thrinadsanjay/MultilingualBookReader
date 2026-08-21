package com.multilingualbookreader.presentation.reader

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.multilingualbookreader.presentation.theme.BookReaderTheme
import org.junit.Rule
import org.junit.Test

class ReaderScreenTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun showsPlaybackControls() {
        composeRule.setContent {
            BookReaderTheme(darkTheme = false, highContrast = false, fontScale = 1.25f) {
                ReaderScreen(
                    state = ReaderUiState(),
                    onBack = {},
                    onSearch = {},
                    onPlay = {},
                    onPause = {},
                    onResume = {},
                    onPrev = {},
                    onNext = {},
                    onSkipParagraph = {},
                    onRepeat = {},
                    onRestart = {},
                    onSpeed = {},
                    onPrevPage = {},
                    onNextPage = {},
                    onBookmark = {},
                    onNoteDraft = {},
                    onSaveNote = {},
                )
            }
        }
        composeRule.onNodeWithText("Play").assertIsDisplayed()
        composeRule.onNodeWithText("1.0x").assertIsDisplayed()
    }
}
