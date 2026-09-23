package com.multilingualbookreader.presentation.reader

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.multilingualbookreader.domain.model.Book
import com.multilingualbookreader.domain.model.BookPage
import com.multilingualbookreader.domain.model.BookSource
import com.multilingualbookreader.domain.model.ProcessingStatus
import com.multilingualbookreader.domain.model.SupportedLanguage
import com.multilingualbookreader.presentation.theme.BookReaderTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class ReaderLayoutTest {
    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun lastPageOffersAddPagesInsteadOfNext() {
        var added = false
        composeRule.setContent {
            BookReaderTheme(darkTheme = true, highContrast = false, fontScale = 1.0f) {
                ReaderScreen(
                    state = ReaderUiState(
                        book = Book(
                            id = "b1",
                            title = "Scanned book",
                            author = null,
                            coverPath = null,
                            sourceType = BookSource.CAMERA_SCAN,
                            language = SupportedLanguage.TELUGU,
                            totalPages = 1,
                            createdAt = 0,
                            updatedAt = 0,
                        ),
                        pages = listOf(
                            BookPage(
                                id = "p1",
                                bookId = "b1",
                                pageNumber = 1,
                                imagePath = null,
                                text = "యథాతథము",
                                language = SupportedLanguage.TELUGU,
                                processingStatus = ProcessingStatus.COMPLETED,
                            ),
                        ),
                    ),
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
                    onAddPages = { added = true },
                )
            }
        }
        composeRule.onNodeWithText("యథాతథము").assertIsDisplayed()
        composeRule.onNodeWithText("1  /  1 · Standard voice").assertIsDisplayed()
        composeRule.onAllNodesWithContentDescription("Add pages")[0].assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Previous page").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNodeWithText("Add a note").assertIsDisplayed()
        composeRule.onNodeWithText("Add pages").performClick()
        assert(added)
    }
}
