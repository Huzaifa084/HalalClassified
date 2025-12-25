package com.halalclassified.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ChatsScreen() {
    PlaceholderScreen(
        title = "Chats",
        description = "Your conversations with sellers will appear here."
    )
}

@Composable
fun SellScreen() {
    PlaceholderScreen(
        title = "Post an Ad",
        description = "Upload photos, add details, and publish your listing."
    )
}

@Composable
fun MyAdsScreen() {
    PlaceholderScreen(
        title = "My Ads",
        description = "Track, edit, and manage your active listings."
    )
}

@Composable
fun ProfileScreen() {
    PlaceholderScreen(
        title = "Profile",
        description = "Manage your account, favorites, and preferences."
    )
}

@Composable
private fun PlaceholderScreen(
    title: String,
    description: String
) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
