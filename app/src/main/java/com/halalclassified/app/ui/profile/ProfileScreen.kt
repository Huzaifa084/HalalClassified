package com.halalclassified.app.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.halalclassified.app.data.ads.AdWithImage
import com.halalclassified.app.data.ads.AdsRepository
import com.halalclassified.app.data.favorites.FavoritesRepository
import com.halalclassified.app.data.profile.Profile
import com.halalclassified.app.data.profile.ProfileRepository
import com.halalclassified.app.data.supabase.SupabaseClientProvider
import com.halalclassified.app.ui.ad.AdDetailScreen
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    onOpenChat: (String) -> Unit
) {
    val supabase = SupabaseClientProvider.client
    val profileRepository = remember { ProfileRepository(supabase) }
    val adsRepository = remember { AdsRepository(supabase) }
    val favoritesRepository = remember { FavoritesRepository(supabase, adsRepository) }
    val sessionStatus by supabase.auth.sessionStatus.collectAsState()
    val user = (sessionStatus as? SessionStatus.Authenticated)?.session?.user
    val userId = user?.id
    val scope = rememberCoroutineScope()

    var profile by remember { mutableStateOf<Profile?>(null) }
    var favorites by remember { mutableStateOf<List<AdWithImage>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedAdId by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(userId) {
        if (userId.isNullOrBlank()) {
            errorMessage = "Log in to view your profile."
            return@LaunchedEffect
        }
        isLoading = true
        errorMessage = null
        profile = runCatching { profileRepository.fetchProfile(userId) }.getOrNull()
        favorites = runCatching { favoritesRepository.fetchFavorites(userId) }
            .getOrDefault(emptyList())
        isLoading = false
    }

    if (selectedAdId != null) {
        AdDetailScreen(
            adId = selectedAdId ?: "",
            isFavorite = true,
            onToggleFavorite = {
                val targetId = selectedAdId ?: return@AdDetailScreen
                val currentUser = userId ?: return@AdDetailScreen
                scope.launch {
                    favoritesRepository.toggleFavorite(currentUser, targetId)
                    favorites = favorites.filterNot { it.ad.id == targetId }
                    selectedAdId = null
                }
            },
            onBack = { selectedAdId = null },
            onOpenChat = onOpenChat
        )
        return
    }

    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Profile",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Manage your account and saved listings.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (errorMessage != null) {
                StatusBanner(message = errorMessage ?: "")
            }

            ProfileHeader(
                name = profileDisplayName(profile, user?.email),
                email = user?.email.orEmpty(),
                phone = profile?.phone.orEmpty()
            )

            Text(
                text = "Favorites",
                style = MaterialTheme.typography.titleLarge
            )

            if (!isLoading && favorites.isEmpty()) {
                StatusBanner(message = "No favorites yet. Tap the heart to save a listing.")
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(favorites, key = { it.ad.id }) { item ->
                    FavoriteCard(
                        item = item,
                        onOpen = { selectedAdId = item.ad.id },
                        onRemove = {
                            val currentUser = userId ?: return@FavoriteCard
                            scope.launch {
                                favoritesRepository.toggleFavorite(currentUser, item.ad.id)
                                favorites = favorites.filterNot { it.ad.id == item.ad.id }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileHeader(name: String, email: String, phone: String) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Icon(
                    imageVector = Icons.Outlined.PersonOutline,
                    contentDescription = "Profile",
                    modifier = Modifier
                        .padding(12.dp)
                        .size(28.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (phone.isNotBlank()) {
                    Text(
                        text = phone,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun FavoriteCard(
    item: AdWithImage,
    onOpen: () -> Unit,
    onRemove: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = item.ad.title,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.ad.title?.takeIf { it.isNotBlank() } ?: "Listing",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.ad.city?.takeIf { it.isNotBlank() } ?: "Pakistan",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = "Remove favorite",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun StatusBanner(message: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun profileDisplayName(profile: Profile?, email: String?): String {
    val first = profile?.firstName?.trim().orEmpty()
    val last = profile?.lastName?.trim().orEmpty()
    return listOf(first, last).filter { it.isNotBlank() }.joinToString(" ").ifBlank {
        email ?: "Account"
    }
}
