package com.multilingualbookreader.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.multilingualbookreader.navigation.TabCommand
import com.multilingualbookreader.navigation.TabNavigationPolicy
import com.multilingualbookreader.presentation.components.AppTab
import com.multilingualbookreader.presentation.components.ReaderBottomBar
import com.multilingualbookreader.presentation.components.appTabForRoute
import com.multilingualbookreader.presentation.home.HomeRoute
import com.multilingualbookreader.presentation.library.LibraryRoute
import com.multilingualbookreader.presentation.pdf.PdfImportRoute
import com.multilingualbookreader.presentation.reader.ReaderRoute
import com.multilingualbookreader.presentation.scan.ScanRoute
import com.multilingualbookreader.presentation.search.SearchRoute
import com.multilingualbookreader.presentation.settings.AboutRoute
import com.multilingualbookreader.presentation.settings.HelpRoute
import com.multilingualbookreader.presentation.settings.PrivacyRoute
import com.multilingualbookreader.presentation.settings.ServerRoute
import com.multilingualbookreader.presentation.settings.SettingsRoute
import com.multilingualbookreader.presentation.settings.UpdatesRoute
import com.multilingualbookreader.presentation.voice.VoiceRoute
import com.multilingualbookreader.presentation.voice.VoiceTestRoute

object Routes {
    const val Home = "home"
    const val Library = "library"
    const val Settings = "settings"
    const val Privacy = "privacy"
    const val Server = "server"
    const val Updates = "updates"
    const val Help = "help"
    const val About = "about"
    const val Scan = "scan?bookId={bookId}"
    const val PdfImport = "pdf"
    const val Reader = "reader/{bookId}"
    const val Voice = "voice"
    const val VoiceTest = "voice-test"
    const val Search = "search/{bookId}"
}

@Composable
fun BookReaderNavHost() {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val showBottomBar = appTabForRoute(route) != null
    val onScan = route?.startsWith("scan") == true

    Scaffold(
        containerColor = if (onScan) Color(0xFF0E0E11) else MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                ReaderBottomBar(currentRoute = route) { tab ->
                    val command = TabNavigationPolicy.onTabSelected(
                        currentRoute = route,
                        currentTabRoute = appTabForRoute(route)?.route,
                        targetTabRoute = tab.route,
                    )
                    when (command) {
                        // Already the visible root; navigating again would stack a duplicate.
                        TabCommand.Stay -> Unit
                        // One step back to the tab root, so Settings never lands on Updates.
                        is TabCommand.ReturnToTabRoot -> nav.popBackStack(command.route, inclusive = false)
                        is TabCommand.SwitchTab -> nav.navigate(command.route) {
                            popUpTo(nav.graph.findStartDestination().id) { saveState = false }
                            launchSingleTop = true
                            restoreState = false
                        }
                    }
                }
            }
        },
    ) { padding ->
        NavHost(navController = nav, startDestination = Routes.Home, modifier = Modifier.padding(padding)) {
            composable(Routes.Home) {
                HomeRoute(
                    onScan = { nav.navigate("scan?bookId=") },
                    onImportPdf = { nav.navigate(Routes.PdfImport) },
                    onLibrary = { nav.navigate(AppTab.Library.route) },
                    onVoice = { nav.navigate(AppTab.Voice.route) },
                    onContinue = { bookId -> nav.navigate("reader/$bookId") },
                    onUpdates = { nav.navigate(Routes.Updates) },
                )
            }
            composable(Routes.Library) {
                LibraryRoute(
                    onOpen = { nav.navigate("reader/$it") },
                    onBack = { nav.popBackStack() },
                    onScan = { nav.navigate("scan?bookId=") },
                    onImportPdf = { nav.navigate(Routes.PdfImport) },
                    showBack = false,
                )
            }
            composable(Routes.Settings) {
                SettingsRoute(
                    onPrivacy = { nav.navigate(Routes.Privacy) },
                    onVoiceTest = { nav.navigate(Routes.VoiceTest) },
                    onUpdates = { nav.navigate(Routes.Updates) },
                    onServer = { nav.navigate(Routes.Server) },
                    onHelp = { nav.navigate(Routes.Help) },
                    onAbout = { nav.navigate(Routes.About) },
                    onBack = { nav.popBackStack() },
                    showBack = false,
                )
            }
            composable(Routes.Server) { ServerRoute(onBack = { nav.popBackStack() }) }
            composable(Routes.Privacy) { PrivacyRoute(onBack = { nav.popBackStack() }) }
            composable(Routes.Updates) { UpdatesRoute(onBack = { nav.popBackStack() }) }
            composable(Routes.Help) { HelpRoute(onBack = { nav.popBackStack() }) }
            composable(Routes.About) { AboutRoute(onBack = { nav.popBackStack() }) }
            composable(
                route = Routes.Scan,
                arguments = listOf(navArgument("bookId") { type = NavType.StringType; defaultValue = "" }),
            ) {
                ScanRoute(
                    onOpenReader = { nav.navigate("reader/$it") { popUpTo(Routes.Home) } },
                    onImportPdf = { nav.navigate(Routes.PdfImport) },
                    onOpenServer = { nav.navigate(Routes.Server) },
                    onBack = { nav.popBackStack() },
                )
            }
            composable(Routes.PdfImport) {
                PdfImportRoute(
                    onOpenReader = { nav.navigate("reader/$it") { popUpTo(Routes.Home) } },
                    onBack = { nav.popBackStack() },
                )
            }
            composable(
                route = Routes.Reader,
                arguments = listOf(navArgument("bookId") { type = NavType.StringType }),
            ) {
                val bookId = it.arguments?.getString("bookId").orEmpty()
                ReaderRoute(
                    bookId = bookId,
                    onBack = { nav.popBackStack() },
                    onSearch = { nav.navigate("search/$bookId") },
                )
            }
            composable(Routes.Voice) {
                VoiceRoute(onBack = { nav.popBackStack() }, onTest = { nav.navigate(Routes.VoiceTest) }, showBack = false)
            }
            composable(Routes.VoiceTest) { VoiceTestRoute(onBack = { nav.popBackStack() }) }
            composable(
                route = Routes.Search,
                arguments = listOf(navArgument("bookId") { type = NavType.StringType }),
            ) {
                SearchRoute(
                    bookId = it.arguments?.getString("bookId").orEmpty(),
                    onOpenPage = { _ -> nav.popBackStack() },
                    onBack = { nav.popBackStack() },
                )
            }
        }
    }
}
