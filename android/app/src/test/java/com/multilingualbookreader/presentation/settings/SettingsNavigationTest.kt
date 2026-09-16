package com.multilingualbookreader.presentation.settings

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.multilingualbookreader.domain.model.OcrRoute
import com.multilingualbookreader.domain.model.ThemeMode
import com.multilingualbookreader.presentation.theme.BookReaderTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Settings used to jump straight to Updates because the navigation lambda was bound to a composable
 * slot and therefore ran while the screen composed. These tests pin the corrected behaviour.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class SettingsNavigationTest {
    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

    private var updatesOpened = 0
    private var privacyOpened = 0

    private fun showSettings() {
        composeRule.setContent {
            BookReaderTheme(darkTheme = false, highContrast = false, fontScale = 1.0f) {
                SettingsScreen(
                    theme = ThemeMode.SYSTEM,
                    fontScale = 1.0f,
                    highContrast = false,
                    reduceMotion = false,
                    ocrRoute = OcrRoute.AUTO,
                    analytics = false,
                    crash = false,
                    onTheme = {},
                    onFont = {},
                    onContrast = {},
                    onMotion = {},
                    onOcr = {},
                    onAnalytics = {},
                    onCrash = {},
                    onPrivacy = { privacyOpened++ },
                    onVoiceTest = {},
                    onUpdates = { updatesOpened++ },
                    onBack = {},
                    showBack = false,
                )
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun openingSettingsDoesNotNavigateAnywhere() {
        showSettings()
        assertThat(updatesOpened).isEqualTo(0)
        assertThat(privacyOpened).isEqualTo(0)
        composeRule.onNodeWithText("Settings").assertExists()
    }

    @Test
    fun changingAThemePreferenceStillDoesNotOpenUpdates() {
        showSettings()
        composeRule.onNodeWithText("Theme").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Dark").performClick()
        composeRule.waitForIdle()
        assertThat(updatesOpened).isEqualTo(0)
    }

    @Test
    fun updatesOpenOnlyWhenTheRowIsTapped() {
        showSettings()
        composeRule.onNodeWithText("Check for updates").performClick()
        composeRule.waitForIdle()
        assertThat(updatesOpened).isEqualTo(1)
        assertThat(privacyOpened).isEqualTo(0)
    }
}
