package com.multilingualbookreader.presentation.scan

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.google.common.truth.Truth.assertThat
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
                    onRequestCamera = {},
                    onTextChange = {},
                    onSave = {},
                    onRetake = {},
                    onRead = {},
                    onBack = {},
                )
            }
        }
        composeRule.onNodeWithText("Choose from gallery").assertIsDisplayed()
        composeRule.onNodeWithText("Allow camera").assertIsDisplayed()
        assertThat(composeRule.onAllNodesWithText("Capture page").fetchSemanticsNodes()).isEmpty()
    }
}
