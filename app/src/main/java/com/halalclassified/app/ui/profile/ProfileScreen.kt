package com.halalclassified.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.halalclassified.app.data.ads.AdWithImage
import com.halalclassified.app.data.ads.AdsRepository
import com.halalclassified.app.data.favorites.FavoritesRepository
import com.halalclassified.app.data.profile.Profile
import com.halalclassified.app.data.profile.ProfileRepository
import com.halalclassified.app.data.profile.ProfileUpsert
import com.halalclassified.app.data.supabase.SupabaseClientProvider
import com.halalclassified.app.ui.ad.AdDetailScreen
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.user.UserInfo
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

@Composable
fun ProfileScreen(
    onOpenChat: (String) -> Unit,
    onManageListing: (String) -> Unit
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
    var isSigningOut by remember { mutableStateOf(false) }

    LaunchedEffect(userId) {
        if (userId.isNullOrBlank()) {
            errorMessage = "Log in to view your profile."
            return@LaunchedEffect
        }
        isLoading = true
        errorMessage = null
        val fetchedProfile = runCatching { profileRepository.fetchProfile(userId) }.getOrNull()
        profile = fetchedProfile

        // Best-effort hydration: if the profile table is missing name/email, persist what we already
        // know from auth metadata so Profile looks complete across sessions/devices.
        val desired = deriveProfilePatch(user, fetchedProfile)
        if (desired != null) {
            runCatching {
                profileRepository.upsertProfile(desired)
            }.onSuccess { updated ->
                profile = updated ?: profile
            }
        }

        favorites = runCatching { favoritesRepository.fetchFavorites(userId) }.getOrDefault(emptyList())
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
            onOpenChat = onOpenChat,
            onManageListing = { adId ->
                selectedAdId = null
                onManageListing(adId)
            }
        )
        return
    }

    val headerGradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surface
        )
    )

    Surface(color = MaterialTheme.colorScheme.background) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(headerGradient),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = "Profile",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            item {
                Text(
                    text = "Manage your account and saved listings.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isLoading) {
                item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
            }

            if (errorMessage != null) {
                item { StatusBanner(message = errorMessage ?: "") }
            }

            item {
                val name = profileDisplayName(profile, user)
                val initials = initialsFor(name)
                ProfileHeaderCard(
                    initials = initials,
                    name = name,
                    email = user?.email.orEmpty(),
                    subtitle = when {
                        profile?.city?.isNullOrBlank() == false -> profile?.city.orEmpty()
                        profile?.phone?.isNullOrBlank() == false -> profile?.phone.orEmpty()
                        else -> "Halal Classified member"
                    },
                    isSigningOut = isSigningOut,
                    onSignOut = {
                        if (userId.isNullOrBlank() || isSigningOut) return@ProfileHeaderCard
                        scope.launch {
                            isSigningOut = true
                            errorMessage = null
                            runCatching { supabase.auth.signOut() }
                                .onFailure { error ->
                                    errorMessage = error.message ?: "Unable to sign out. Try again."
                                }
                            isSigningOut = false
                        }
                    }
                )
            }

            item {
                AccountDetailsCard(
                    firstName = deriveFirstName(profile, user),
                    lastName = deriveLastName(profile, user),
                    email = user?.email.orEmpty(),
                    phone = profile?.phone,
                    city = profile?.city,
                    dob = profile?.dob
                )
            }

            item {
                SectionTitleRow(
                    title = "Favorites",
                    count = favorites.size
                )
            }

            if (!isLoading && favorites.isEmpty()) {
                item { StatusBanner(message = "No favorites yet. Tap the heart to save a listing.") }
            }

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

@Composable
private fun ProfileHeaderCard(
    initials: String,
    name: String,
    email: String,
    subtitle: String,
    isSigningOut: Boolean,
    onSignOut: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    tonalElevation = 2.dp,
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name.ifBlank { "Account" },
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (email.isNotBlank()) {
                        Text(
                            text = email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onSignOut,
                    enabled = !isSigningOut,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isSigningOut) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Logout,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    Text(text = if (isSigningOut) "Signing out" else "Log out")
                }

                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = {
                        Text(text = "Secure")
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Badge,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledLeadingIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}

@Composable
private fun AccountDetailsCard(
    firstName: String,
    lastName: String,
    email: String,
    phone: String?,
    city: String?,
    dob: String?
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Account",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (city.isNullOrBlank().not()) {
                    item {
                        AssistChip(
                            onClick = {},
                            enabled = false,
                            label = { Text(text = city.orEmpty()) },
                            colors = AssistChipDefaults.assistChipColors(
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
                if (dob.isNullOrBlank().not()) {
                    item {
                        AssistChip(
                            onClick = {},
                            enabled = false,
                            label = { Text(text = "DOB: ${dob.orEmpty()}") },
                            colors = AssistChipDefaults.assistChipColors(
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }

            DetailRow(
                icon = Icons.Outlined.Person,
                label = "First name",
                value = firstName.ifBlank { "Not set" }
            )
            DetailRow(
                icon = Icons.Outlined.PersonOutline,
                label = "Last name",
                value = lastName.ifBlank { "Not set" }
            )
            DetailRow(
                icon = Icons.Outlined.Email,
                label = "Email",
                value = email.ifBlank { "Not available" }
            )
            if (!phone.isNullOrBlank()) {
                DetailRow(
                    icon = Icons.Outlined.Phone,
                    label = "Phone",
                    value = phone
                )
            }
        }
    }
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .padding(10.dp)
                    .size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SectionTitleRow(title: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        AssistChip(
            onClick = {},
            enabled = false,
            label = { Text(text = count.toString()) },
            colors = AssistChipDefaults.assistChipColors(
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
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
                modifier = Modifier
                    .size(64.dp)
                    .clip(MaterialTheme.shapes.medium)
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

private fun profileDisplayName(profile: Profile?, user: UserInfo?): String {
    val first = deriveFirstName(profile, user)
    val last = deriveLastName(profile, user)
    return listOf(first, last)
        .filter { it.isNotBlank() }
        .joinToString(" ")
        .ifBlank {
            val full = user?.userMetadata?.get("full_name").asStringOrNull()?.trim().orEmpty()
            full.ifBlank { user?.email ?: "Account" }
        }
}

private fun deriveFirstName(profile: Profile?, user: UserInfo?): String {
    val p = profile?.firstName?.trim().orEmpty()
    if (p.isNotBlank()) return p
    val metaFirst = user?.userMetadata?.get("first_name").asStringOrNull()?.trim().orEmpty()
    if (metaFirst.isNotBlank()) return metaFirst
    val full = user?.userMetadata?.get("full_name").asStringOrNull()?.trim().orEmpty()
    return full.split(' ').firstOrNull().orEmpty()
}

private fun deriveLastName(profile: Profile?, user: UserInfo?): String {
    val p = profile?.lastName?.trim().orEmpty()
    if (p.isNotBlank()) return p
    val metaLast = user?.userMetadata?.get("last_name").asStringOrNull()?.trim().orEmpty()
    if (metaLast.isNotBlank()) return metaLast
    val full = user?.userMetadata?.get("full_name").asStringOrNull()?.trim().orEmpty()
    val parts = full.split(' ').filter { it.isNotBlank() }
    return if (parts.size >= 2) parts.drop(1).joinToString(" ") else ""
}

private fun initialsFor(name: String): String {
    val parts = name.trim().split(' ').filter { it.isNotBlank() }
    val first = parts.firstOrNull()?.firstOrNull()?.uppercaseChar()?.toString().orEmpty()
    val second = parts.getOrNull(1)?.firstOrNull()?.uppercaseChar()?.toString().orEmpty()
    return (first + second).ifBlank { "HC" }
}

private fun deriveProfilePatch(user: UserInfo?, profile: Profile?): ProfileUpsert? {
    if (user == null) return null
    val id = user.id
    if (id.isBlank()) return null

    val desiredFirst = deriveFirstName(profile, user).takeIf { it.isNotBlank() }
    val desiredLast = deriveLastName(profile, user).takeIf { it.isNotBlank() }
    val desiredEmail = user.email?.trim()?.takeIf { it.isNotBlank() }

    val needs = (profile?.firstName.isNullOrBlank() && desiredFirst != null) ||
        (profile?.lastName.isNullOrBlank() && desiredLast != null) ||
        (profile?.email.isNullOrBlank() && desiredEmail != null)

    if (!needs) return null
    return ProfileUpsert(
        id = id,
        firstName = if (profile?.firstName.isNullOrBlank()) desiredFirst else null,
        lastName = if (profile?.lastName.isNullOrBlank()) desiredLast else null,
        email = if (profile?.email.isNullOrBlank()) desiredEmail else null
    )
}

private fun JsonElement?.asStringOrNull(): String? = this?.jsonPrimitive?.contentOrNull
