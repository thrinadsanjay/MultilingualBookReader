package com.multilingualbookreader.presentation.library

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.multilingualbookreader.domain.model.Book
import com.multilingualbookreader.domain.model.BookPriority
import com.multilingualbookreader.domain.model.BookSource
import com.multilingualbookreader.domain.model.LibraryBook
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
class LibraryLayoutTest {
    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun cardShowsNameTagsPriorityAndRename() {
        composeRule.setContent {
            BookReaderTheme(darkTheme = true, highContrast = false, fontScale = 1.0f) {
                LibraryScreen(
                    books = listOf(
                        LibraryBook(
                            book = Book(
                                id = "b1",
                                title = "యథాతథము",
                                author = null,
                                coverPath = null,
                                sourceType = BookSource.CAMERA_SCAN,
                                language = SupportedLanguage.TELUGU,
                                totalPages = 2,
                                createdAt = 1_758_000_000_000L,
                                updatedAt = 1_758_000_000_000L,
                                tags = listOf("Temple"),
                                priority = BookPriority.HIGH,
                                color = "#6FBFA8",
                                genre = "Religion",
                            ),
                            completedPages = 2,
                            progressPercent = 50,
                            lastReadAt = null,
                        ),
                    ),
                    onOpen = {},
                    onBack = {},
                    onDelete = {},
                )
            }
        }
        composeRule.onNodeWithText("యథాతథము").assertIsDisplayed()
        composeRule.onNodeWithText("High").assertIsDisplayed()
        composeRule.onNodeWithText("Temple").assertIsDisplayed()
        composeRule.onAllNodesWithText("Religion", substring = true)[0].assertIsDisplayed()
        composeRule.onNodeWithText("All genres").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Book options").performClick()
        composeRule.onNodeWithText("Rename").assertIsDisplayed()
        composeRule.onNodeWithText("Export").assertIsDisplayed()
        composeRule.onNodeWithText("Share").assertIsDisplayed()
        composeRule.onNodeWithText("Genre").assertIsDisplayed()
    }
}
