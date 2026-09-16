package com.multilingualbookreader.presentation.scan

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
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
                    onImportPdf = {},
                )
            }
        }
        composeRule.onNodeWithText("Scan Book").assertIsDisplayed()
        composeRule.onNodeWithText("Align the page inside the frame").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Capture page").assertIsDisplayed().assertIsEnabled()
        composeRule.onNodeWithText("Choose from gallery").assertIsDisplayed()
        composeRule.onNodeWithText("Import PDF instead").assertIsDisplayed()
        composeRule.onNodeWithText("Single Page").assertIsDisplayed()
        composeRule.onNodeWithText("Auto Crop").assertIsDisplayed()
        composeRule.onNodeWithText("High Quality").assertIsDisplayed()
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
                    onImportPdf = {},
                    onRequestCamera = {},
                    onTextChange = {},
                    onSave = {},
                    onRetake = {},
                    onRead = {},
                    onBack = {},
                )
            }
        }
        composeRule.onNodeWithText("Camera access is needed to scan a page.").assertIsDisplayed()
        composeRule.onNodeWithText("Allow camera").assertIsDisplayed()
        composeRule.onNodeWithText("Choose from gallery").assertIsDisplayed()
        composeRule.onNodeWithText("Import PDF instead").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Capture page").assertIsDisplayed().assertIsNotEnabled()
    }
}
