package com.halalclassified.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.halalclassified.app.ui.chats.ChatsScreen
import com.halalclassified.app.ui.home.HomeScreen
import com.halalclassified.app.ui.myads.MyAdsScreen
import com.halalclassified.app.ui.profile.ProfileScreen
import com.halalclassified.app.ui.sell.SellScreen

@Composable
fun HalalClassifiedApp() {
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.Home) }
    var pendingChatId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingManageAdId by rememberSaveable { mutableStateOf<String?>(null) }
    val openChat: (String) -> Unit = { chatId ->
        pendingChatId = chatId
        selectedTab = AppTab.Chats
    }
    val openManageListing: (String) -> Unit = { adId ->
        pendingChatId = null
        pendingManageAdId = adId
        selectedTab = AppTab.MyAds
    }

    BackHandler(enabled = selectedTab != AppTab.Home) {
        pendingChatId = null
        pendingManageAdId = null
        selectedTab = AppTab.Home
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            AppBottomBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                AppTab.Home -> HomeScreen(
                    onOpenChat = openChat,
                    onManageListing = openManageListing
                )
                AppTab.Chats -> ChatsScreen(
                    startChatId = pendingChatId,
                    onChatConsumed = { pendingChatId = null }
                )
                AppTab.Sell -> SellScreen()
                AppTab.MyAds -> MyAdsScreen(
                    startEditAdId = pendingManageAdId,
                    onEditConsumed = { pendingManageAdId = null }
                )
                AppTab.Profile -> ProfileScreen(
                    onOpenChat = openChat,
                    onManageListing = openManageListing
                )
            }
        }
    }
}

private enum class AppTab(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
) {
    Home("Home", Icons.Outlined.Home, Icons.Filled.Home),
    Chats("Chats", Icons.Outlined.ChatBubbleOutline, Icons.Filled.ChatBubble),
    Sell("Sell", Icons.Outlined.AddCircleOutline, Icons.Filled.AddCircle),
    MyAds("My Ads", Icons.Outlined.ListAlt, Icons.Filled.ListAlt),
    Profile("Profile", Icons.Outlined.PersonOutline, Icons.Filled.Person)
}

@Composable
private fun AppBottomBar(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        AppTab.entries.forEach { tab ->
            val selected = tab == selectedTab
            NavigationBarItem(
                selected = selected,
                onClick = { onTabSelected(tab) },
                icon = {
                    androidx.compose.material3.Icon(
                        imageVector = if (selected) tab.selectedIcon else tab.icon,
                        contentDescription = tab.label
                    )
                },
                label = { androidx.compose.material3.Text(text = tab.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
