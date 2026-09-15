package com.multilingualbookreader.presentation.scan

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.google.common.truth.Truth.assertThat
import com.multilingualbookreader.presentation.theme.BookReaderTheme
import org.junit.Rule
import org.junit.Test

class ScanReviewTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun cameraPaneOffersGalleryPick() {
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
                    onPickGallery = {},
                )
            }
        }
        composeRule.onNodeWithText("Scan Book").assertIsDisplayed()
        composeRule.onNodeWithText("Align the page inside the frame").assertIsDisplayed()
        composeRule.onNodeWithText("Capture page").assertIsDisplayed()
        composeRule.onNodeWithText("Choose from gallery").assertIsDisplayed()
    }

    @Test
    fun galleryIsAvailableWhenCameraIsDenied() {
        composeRule.setContent {
            BookReaderTheme(darkTheme = false, highContrast = false, fontScale = 1.25f) {
                ScanScreen(
                    state = ScanUiState(),
                    cameraGranted = false,
                    onCapture = {},
                    onPickGallery = {},
                    onRequestCamera = {},
                    onTextChange = {},
                    onSave = {},
                    onRetake = {},
                    onRead = {},
                    onBack = {},
                )
            }
        }
        composeRule.onNodeWithText("Scan a page with the camera, or pick a photo from your gallery.").assertIsDisplayed()
        composeRule.onNodeWithText("Allow camera").assertIsDisplayed()
        composeRule.onNodeWithText("Choose from gallery").assertIsDisplayed()
        assertThat(composeRule.onAllNodesWithText("Align the page inside the frame").fetchSemanticsNodes()).isEmpty()
        assertThat(composeRule.onAllNodesWithText("Capture page").fetchSemanticsNodes()).isEmpty()
    }
}
