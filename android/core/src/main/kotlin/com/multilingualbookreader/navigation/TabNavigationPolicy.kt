package com.multilingualbookreader.navigation

sealed interface TabCommand {
    /** Already at the tab root; pushing it again would stack duplicates. */
    data object Stay : TabCommand

    /** Inside the tab but on a child screen, so drop back to the tab's root in one step. */
    data class ReturnToTabRoot(val route: String) : TabCommand

    data class SwitchTab(val route: String) : TabCommand
}

object TabNavigationPolicy {
    /**
     * Decides what tapping a bottom-navigation tab should do.
     *
     * @param currentRoute the route on screen right now, for example "updates".
     * @param currentTabRoute the tab that route belongs to, for example "settings".
     * @param targetTabRoute the tab the user tapped.
     */
    fun onTabSelected(
        currentRoute: String?,
        currentTabRoute: String?,
        targetTabRoute: String,
    ): TabCommand = when {
        currentRoute == targetTabRoute -> TabCommand.Stay
        currentTabRoute == targetTabRoute -> TabCommand.ReturnToTabRoot(targetTabRoute)
        else -> TabCommand.SwitchTab(targetTabRoute)
    }
}
