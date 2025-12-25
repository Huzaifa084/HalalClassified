package com.halalclassified.app.ui.ad

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role
import coil.compose.AsyncImage
import com.halalclassified.app.data.ads.AdDetail
import com.halalclassified.app.data.ads.AdsRepository
import com.halalclassified.app.data.chat.ChatRepository
import com.halalclassified.app.data.profile.Profile
import com.halalclassified.app.data.profile.ProfileRepository
import com.halalclassified.app.data.supabase.SupabaseClientProvider
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun AdDetailScreen(
    adId: String,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onBack: () -> Unit,
    onOpenChat: (String) -> Unit,
    onManageListing: (String) -> Unit
) {
    val context = LocalContext.current
    val supabase = SupabaseClientProvider.client
    val adsRepository = remember { AdsRepository(supabase) }
    val profileRepository = remember { ProfileRepository(supabase) }
    val chatRepository = remember { ChatRepository(supabase) }
    val scope = rememberCoroutineScope()
    val sessionStatus by supabase.auth.sessionStatus.collectAsState()
    val userId = (sessionStatus as? SessionStatus.Authenticated)?.session?.user?.id

    var detailState by remember { mutableStateOf<AdDetail?>(null) }
    var sellerProfile by remember { mutableStateOf<Profile?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(adId) {
        isLoading = true
        errorMessage = null
        detailState = runCatching { adsRepository.fetchAdDetail(adId) }
            .onFailure { error ->
                errorMessage = error.message ?: "Unable to load this listing."
            }
            .getOrNull()
        val sellerId = detailState?.ad?.userId
        sellerProfile = if (!sellerId.isNullOrBlank()) {
            runCatching { profileRepository.fetchProfile(sellerId) }.getOrNull()
        } else {
            null
        }
        isLoading = false
    }

    Surface(color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowBack,
                        contentDescription = "Back"
                    )
                }
                Text(
                    text = "Listing",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite"
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 20.dp)
            ) {
                if (isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                if (errorMessage != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    return@Surface
                }

                val detail = detailState ?: return@Surface
                val ad = detail.ad
                val isOwner = !userId.isNullOrBlank() && ad.userId == userId
                val canChat = !userId.isNullOrBlank() && !isOwner && !ad.userId.isNullOrBlank()

                if (detail.imageUrls.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(detail.imageUrls, key = { it }) { url ->
                            AsyncImage(
                                model = url,
                                contentDescription = ad.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp)
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.secondaryContainer
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Image,
                            contentDescription = "Listing image",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                Text(
                    text = formatPrice(ad.price),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = ad.title?.takeIf { it.isNotBlank() }
                        ?: ad.breed?.takeIf { it.isNotBlank() }
                        ?: "Premium listing",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "${ad.category ?: "Animal"} - ${ad.city ?: "Pakistan"} - ${formatRelativeTime(ad.createdAt)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!ad.description.isNullOrBlank()) {
                    Text(
                        text = ad.description,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                InfoRow(label = "Breed", value = ad.breed)
                InfoRow(label = "Gender", value = ad.gender)
                InfoRow(label = "Age", value = ad.age)
                InfoRow(label = "Weight", value = ad.weight)
                InfoRow(label = "Vaccination", value = ad.vaccinationStatus)
                InfoRow(label = "Delivery", value = ad.deliveryAvailable)

                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            enabled = canChat,
                            role = Role.Button
                        ) {
                            val sellerId = ad.userId ?: return@clickable
                            scope.launch {
                                val chat = chatRepository.getOrCreateChat(
                                    adId = ad.id,
                                    buyerId = userId ?: return@launch,
                                    sellerId = sellerId
                                )
                                onOpenChat(chat.id)
                            }
                        }
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Seller",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = buildSellerName(sellerProfile),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        val phone = ad.phone?.takeIf { it.isNotBlank() } ?: sellerProfile?.phone
                        if (!phone.isNullOrBlank()) {
                            Text(
                                text = phone,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isOwner) {
                            Text(
                                text = "This is your listing. You can manage it from My Ads.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else if (canChat) {
                            Text(
                                text = "Tap to chat with the seller.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (isOwner) {
                    Button(
                        onClick = { onManageListing(ad.id) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Manage listing"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Manage listing")
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                val sellerId = ad.userId ?: return@Button
                                scope.launch {
                                    val chat = chatRepository.getOrCreateChat(
                                        adId = ad.id,
                                        buyerId = userId ?: return@launch,
                                        sellerId = sellerId
                                    )
                                    onOpenChat(chat.id)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = canChat,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ChatBubbleOutline,
                                contentDescription = "Chat"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Chat")
                        }
                        Button(
                            onClick = {
                                val phone = ad.phone?.takeIf { it.isNotBlank() } ?: sellerProfile?.phone
                                if (!phone.isNullOrBlank()) {
                                    copyAndDial(context, phone)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !ad.phone.isNullOrBlank() || !sellerProfile?.phone.isNullOrBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Call,
                                contentDescription = "Call"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Call")
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
private fun InfoRow(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun buildSellerName(profile: Profile?): String {
    val first = profile?.firstName?.trim().orEmpty()
    val last = profile?.lastName?.trim().orEmpty()
    return listOf(first, last).filter { it.isNotBlank() }.joinToString(" ").ifBlank {
        profile?.email ?: "Seller"
    }
}

private fun formatPrice(raw: String?): String {
    val value = raw?.trim().orEmpty()
    if (value.isBlank()) return "Price on request"
    if (value.contains("pkr", ignoreCase = true)) return value
    val numeric = value.toDoubleOrNull()
    return if (numeric != null) {
        val whole = numeric.toLong()
        "PKR ${String.format(Locale.US, "%,d", whole)}"
    } else {
        value
    }
}

private fun formatRelativeTime(raw: String?): String {
    if (raw.isNullOrBlank()) return "Just now"
    val instant = runCatching { Instant.parse(raw) }.getOrNull() ?: return raw.take(10)
    val duration = Duration.between(instant, Instant.now())
    val minutes = duration.toMinutes()
    val hours = duration.toHours()
    val days = duration.toDays()
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        else -> DateTimeFormatter.ofPattern("dd MMM", Locale.US)
            .withZone(ZoneId.systemDefault())
            .format(instant)
    }
}

private fun copyAndDial(context: Context, phone: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("phone", phone))
    val intent = Intent(Intent.ACTION_DIAL).apply {
        data = Uri.parse("tel:$phone")
    }
    context.startActivity(intent)
}
