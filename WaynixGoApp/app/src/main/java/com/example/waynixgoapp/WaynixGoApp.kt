package com.example.waynixgoapp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.waynixgoapp.ui.navigation.WaynixBottomBar
import com.example.waynixgoapp.ui.screens.*

// MAIN APP ENTRY
@Composable
fun WaynixGoApp(
    onLogout: () -> Unit = {},
    onLanguageChange: (String) -> Unit = {}
) {
    // 0 = Home, 1 = MyAds, 2 = Post (central FAB), 3 = Profile
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        containerColor = com.example.waynixgoapp.ui.theme.WaynixColors.Background,
        bottomBar = {
            WaynixBottomBar(
                selected = selectedTab,
                onSelect = { selectedTab = it }
            )
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> HomeScreen(onProfileClick = { selectedTab = 3 })
                1 -> MyAdsScreen(onProfileClick = { selectedTab = 3 })
                2 -> PostScreen(onClose = { selectedTab = 0 }, onPublish = { _, _, _, _ -> selectedTab = 0 })
                3 -> ProfileScreen(onBack = { selectedTab = 0 }, onLogout = onLogout, onLanguageChange = onLanguageChange)
            }
        }
    }
}