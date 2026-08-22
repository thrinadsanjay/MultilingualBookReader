package com.multilingualbookreader.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
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
    val icon: ImageVector,
) {
    Home("home", "Home", Icons.Outlined.Home),
    Library("library", "Library", Icons.AutoMirrored.Outlined.MenuBook),
    Voice("voice", "Voice", Icons.Outlined.RecordVoiceOver),
    Settings("settings", "Settings", Icons.Outlined.Settings),
}

fun appTabForRoute(route: String?): AppTab? = AppTab.entries.firstOrNull { it.route == route }

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
                icon = {
                    if (isSelected) {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(brand.accent)
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(tab.icon, contentDescription = tab.label, tint = brand.onAccent, modifier = Modifier.size(20.dp))
                        }
                    } else {
                        Icon(tab.icon, contentDescription = tab.label, tint = brand.textSecondary)
                    }
                },
                label = { Text(tab.label, style = MaterialTheme.typography.labelMedium) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = brand.onAccent,
                    selectedTextColor = brand.accent,
                    indicatorColor = Color.Transparent,
                    unselectedIconColor = brand.textSecondary,
                    unselectedTextColor = brand.textSecondary,
                ),
            )
        }
    }
}
