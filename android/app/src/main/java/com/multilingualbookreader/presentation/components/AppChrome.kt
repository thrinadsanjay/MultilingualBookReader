package com.multilingualbookreader.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.multilingualbookreader.presentation.theme.LocalBrand

enum class AppTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    Home("home", "Home", Icons.Filled.Home, Icons.Outlined.Home),
    Library("library", "Library", Icons.AutoMirrored.Filled.MenuBook, Icons.AutoMirrored.Outlined.MenuBook),
    Voice("voice", "Voice", Icons.Filled.RecordVoiceOver, Icons.Outlined.RecordVoiceOver),
    Settings("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings),
}

fun appTabForRoute(route: String?): AppTab? = AppTab.entries.firstOrNull { tab ->
    route == tab.route
}

@Composable
fun ReaderBottomBar(
    currentRoute: String?,
    onSelect: (AppTab) -> Unit,
) {
    val brand = LocalBrand.current
    val selected = appTabForRoute(currentRoute)
    NavigationBar(containerColor = brand.navBar, tonalElevation = 0.dp) {
        AppTab.entries.forEach { tab ->
            val isSelected = tab == selected
            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelect(tab) },
                alwaysShowLabel = false,
                icon = {
                    if (isSelected) {
                        Box(
                            Modifier
                                .size(width = 48.dp, height = 32.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(brand.orange.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(tab.selectedIcon, contentDescription = tab.label, tint = brand.orange)
                        }
                    } else {
                        Icon(tab.unselectedIcon, contentDescription = tab.label, tint = brand.muted)
                    }
                },
                label = {
                    Text(tab.label, style = MaterialTheme.typography.labelMedium)
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = brand.orange,
                    selectedTextColor = brand.orange,
                    indicatorColor = Color.Transparent,
                    unselectedIconColor = brand.muted,
                    unselectedTextColor = brand.muted,
                ),
            )
        }
    }
}
