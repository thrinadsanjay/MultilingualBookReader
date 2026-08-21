package com.multilingualbookreader.presentation.scan

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.multilingualbookreader.presentation.theme.BookReaderTheme
import org.junit.Rule
import org.junit.Test

class ScanReviewTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun reviewShowsEditorWhenPreviewMissingUsesCameraCopy() {
        composeRule.setContent {
            BookReaderTheme(darkTheme = false, highContrast = false, fontScale = 1.25f) {
                ScanScreen(
                    state = ScanUiState(ocrText = "రాముడు", busy = false),
                    onCapture = {},
                    onTextChange = {},
                    onSave = {},
                    onRetake = {},
                    onRead = {},
                    onBack = {},
                )
            }
        }
        composeRule.onNodeWithText("Scan Book").assertIsDisplayed()
        composeRule.onNodeWithText("Align the page inside the frame").assertIsDisplayed()
    }
}
