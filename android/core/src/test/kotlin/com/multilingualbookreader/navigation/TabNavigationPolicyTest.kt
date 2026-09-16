package com.multilingualbookreader.navigation

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TabNavigationPolicyTest {
    @Test
    fun settingsTabFromUpdatesReturnsToSettingsRoot() {
        val command = TabNavigationPolicy.onTabSelected(
            currentRoute = "updates",
            currentTabRoute = "settings",
            targetTabRoute = "settings",
        )
        assertThat(command).isEqualTo(TabCommand.ReturnToTabRoot("settings"))
    }

    @Test
    fun tappingTheCurrentTabRootDoesNotStackAnotherCopy() {
        val command = TabNavigationPolicy.onTabSelected(
            currentRoute = "settings",
            currentTabRoute = "settings",
            targetTabRoute = "settings",
        )
        assertThat(command).isEqualTo(TabCommand.Stay)
    }

    @Test
    fun anotherTabSwitchesToItsRoot() {
        val command = TabNavigationPolicy.onTabSelected(
            currentRoute = "home",
            currentTabRoute = "home",
            targetTabRoute = "settings",
        )
        assertThat(command).isEqualTo(TabCommand.SwitchTab("settings"))
    }

    @Test
    fun settingsTabFromAnotherTabsChildSwitchesToSettings() {
        val command = TabNavigationPolicy.onTabSelected(
            currentRoute = "voice-test",
            currentTabRoute = "voice",
            targetTabRoute = "settings",
        )
        assertThat(command).isEqualTo(TabCommand.SwitchTab("settings"))
    }
}
