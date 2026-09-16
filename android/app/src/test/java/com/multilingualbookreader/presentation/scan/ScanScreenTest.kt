package com.multilingualbookreader.presentation.scan

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
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
class ScanScreenTest {
    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun galleryChooserIsShownWhenCameraIsDenied() {
        composeRule.setContent {
            BookReaderTheme(darkTheme = false, highContrast = false, fontScale = 1.0f) {
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
        composeRule.onNodeWithText("Scan Book").assertIsDisplayed()
        composeRule.onNodeWithText("Position the page inside the frame").assertIsDisplayed()
        composeRule.onNodeWithText("Keep the page flat, well lit, and avoid shadows.").assertIsDisplayed()
        composeRule.onNodeWithText("Camera access is needed to scan a page.").assertIsDisplayed()
        composeRule.onNodeWithText("Allow camera").assertIsDisplayed()
        composeRule.onNodeWithText("Choose from gallery").assertIsDisplayed()
        composeRule.onNodeWithText("Import PDF instead").assertIsDisplayed()
        composeRule.onNodeWithText("Single Page").assertIsDisplayed()
        composeRule.onNodeWithText("Multiple Pages").assertIsDisplayed()
        composeRule.onNodeWithText("Book Mode").assertIsDisplayed()
        composeRule.onNodeWithText("Align the page inside the frame").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Capture page").assertIsDisplayed().assertIsNotEnabled()
        composeRule.onNodeWithText("Auto Crop").assertIsDisplayed()
        composeRule.onNodeWithText("High Quality").assertIsDisplayed()
    }

    @Test
    fun preparePaneShowsPagesSideBySideWithEditActions() {
        val first = android.graphics.Bitmap.createBitmap(32, 48, android.graphics.Bitmap.Config.ARGB_8888)
        val second = android.graphics.Bitmap.createBitmap(32, 48, android.graphics.Bitmap.Config.ARGB_8888)
        composeRule.setContent {
            BookReaderTheme(darkTheme = true, highContrast = false, fontScale = 1.0f) {
                ScanScreen(
                    state = ScanUiState(
                        drafts = listOf(PageDraft.from(first), PageDraft.from(second)),
                        selectedDraftIndex = 0,
                    ),
                    onCapture = {},
                    onTextChange = {},
                    onSave = {},
                    onRetake = {},
                    onRead = {},
                    onBack = {},
                )
            }
        }
        composeRule.onNodeWithText("Prepare pages").assertIsDisplayed()
        composeRule.onNodeWithText("2 photos").assertIsDisplayed()
        composeRule.onNodeWithText("Rotate").assertIsDisplayed()
        composeRule.onNodeWithText("Crop").assertIsDisplayed()
        composeRule.onNodeWithText("Enhance").assertIsDisplayed()
        composeRule.onNodeWithText("Detect text").assertIsDisplayed()
        composeRule.onNodeWithText("Detect all pages").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Page 1").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Page 2").assertIsDisplayed()
    }
}
