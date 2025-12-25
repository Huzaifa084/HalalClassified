package com.halalclassified.app.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.halalclassified.app.data.ads.AdWithImage
import com.halalclassified.app.data.ads.AdsQuery
import com.halalclassified.app.data.ads.AdsRepository
import com.halalclassified.app.data.favorites.FavoritesRepository
import com.halalclassified.app.data.supabase.SupabaseClientProvider
import com.halalclassified.app.ui.ad.AdDetailScreen
import com.halalclassified.app.ui.common.AllPakistan
import com.halalclassified.app.ui.common.AnimalCategoryOptions
import com.halalclassified.app.ui.common.CityPickerDialog
import com.halalclassified.app.ui.common.PakistanCities
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onOpenChat: (String) -> Unit,
    onManageListing: (String) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedCity by rememberSaveable { mutableStateOf(AllPakistan) }
    var showCityPicker by remember { mutableStateOf(false) }
    var revealContent by remember { mutableStateOf(false) }
    var feedState by remember { mutableStateOf(HomeFeedState()) }
    var refreshKey by remember { mutableStateOf(0) }
    var favoriteIds by remember { mutableStateOf(setOf<String>()) }
    var selectedAdId by remember { mutableStateOf<String?>(null) }

    val supabase = SupabaseClientProvider.client
    val adsRepository = remember { AdsRepository(supabase) }
    val favoritesRepository = remember { FavoritesRepository(supabase, adsRepository) }
    val sessionStatus by supabase.auth.sessionStatus.collectAsState()
    val userId = (sessionStatus as? SessionStatus.Authenticated)?.session?.user?.id
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        delay(120)
        revealContent = true
    }

    LaunchedEffect(userId) {
        if (!userId.isNullOrBlank()) {
            favoriteIds = runCatching { favoritesRepository.fetchFavoriteAdIds(userId) }
                .getOrDefault(emptyList())
                .toSet()
        } else {
            favoriteIds = emptySet()
        }
    }

    LaunchedEffect(query, selectedCategory, selectedCity, refreshKey) {
        if (query.isNotBlank()) {
            delay(320)
        }
        feedState = feedState.copy(isLoading = true, errorMessage = null)
        val queryPayload = AdsQuery(
            search = query.trim(),
            category = selectedCategory,
            city = selectedCity.takeUnless { it == AllPakistan }
        )
        val result = runCatching { adsRepository.fetchFeed(queryPayload) }
        feedState = result.fold(
            onSuccess = { items ->
                feedState.copy(
                    isLoading = false,
                    ads = items.map { it.toUiModel() },
                    errorMessage = null
                )
            },
            onFailure = { error ->
                feedState.copy(
                    isLoading = false,
                    ads = emptyList(),
                    errorMessage = error.message ?: "Unable to load listings right now."
                )
            }
        )
    }

    val gradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surface
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(gradient)
    ) {
        if (selectedAdId != null) {
            AdDetailScreen(
                adId = selectedAdId ?: "",
                isFavorite = selectedAdId in favoriteIds,
                onToggleFavorite = {
                    val currentUser = userId ?: return@AdDetailScreen
                    val targetId = selectedAdId ?: return@AdDetailScreen
                    scope.launch {
                        val nowFavorite = favoritesRepository.toggleFavorite(currentUser, targetId)
                        favoriteIds = if (nowFavorite) {
                            favoriteIds + targetId
                        } else {
                            favoriteIds - targetId
                        }
                    }
                },
                onBack = { selectedAdId = null },
                onOpenChat = onOpenChat,
                onManageListing = { adId ->
                    selectedAdId = null
                    onManageListing(adId)
                }
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                item {
                    AnimatedVisibility(
                        visible = revealContent,
                        enter = fadeIn(animationSpec = tween(500)) +
                                slideInVertically(animationSpec = tween(500)) { it / 3 }
                    ) {
                        HomeHero()
                    }
                }

                item {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search cows, goats, breeds") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = "Search"
                            )
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Tune,
                                contentDescription = "Filters"
                            )
                        },
                        shape = MaterialTheme.shapes.large,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }

                item {
                    AnimatedVisibility(visible = feedState.isLoading) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (feedState.errorMessage != null) {
                    item {
                        ErrorBanner(
                            message = feedState.errorMessage ?: "Something went wrong.",
                            onRetry = { refreshKey += 1 }
                        )
                    }
                }

                item {
                    SectionHeader(
                        title = "Categories",
                        action = "View all"
                    )
                }

                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(categoryItems, key = { it.name }) { category ->
                            CategoryPill(
                                category = category,
                                selected = selectedCategory == category.filterValue,
                                onClick = {
                                    selectedCategory = if (selectedCategory == category.filterValue) {
                                        null
                                    } else {
                                        category.filterValue
                                    }
                                }
                            )
                        }
                    }
                }

                item {
                    CityFilter(
                        selectedCity = selectedCity,
                        onChange = { showCityPicker = true }
                    )
                }

                item {
                    val action = if (feedState.ads.isNotEmpty()) {
                        "${feedState.ads.size} ads"
                    } else {
                        "Latest"
                    }
                    SectionHeader(
                        title = "Listings",
                        action = action
                    )
                }

                items(feedState.ads, key = { it.id }) { ad ->
                    AdCard(
                        ad = ad,
                        isFavorite = ad.id in favoriteIds,
                        onToggleFavorite = {
                            val currentUser = userId ?: return@AdCard
                            scope.launch {
                                val nowFavorite = favoritesRepository.toggleFavorite(currentUser, ad.id)
                                favoriteIds = if (nowFavorite) {
                                    favoriteIds + ad.id
                                } else {
                                    favoriteIds - ad.id
                                }
                            }
                        },
                        onOpen = { selectedAdId = ad.id }
                    )
                }

                if (!feedState.isLoading && feedState.ads.isEmpty() && feedState.errorMessage == null) {
                    item {
                        EmptyStateCard(
                            title = "No listings yet",
                            subtitle = "Try another city or clear the search to explore more animals."
                        )
                    }
                }
            }
        }

        if (showCityPicker) {
            CityPickerDialog(
                cities = PakistanCities,
                selectedCity = selectedCity,
                includeAllPakistan = true,
                onSelect = { city ->
                    selectedCity = city
                    showCityPicker = false
                },
                onDismiss = { showCityPicker = false }
            )
        }
    }
}

@Composable
private fun HomeHero() {
    val gradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primaryContainer
        )
    )

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = Color.Transparent,
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .background(gradient)
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Assalamualaikum",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Find your Qurbani animal",
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Trusted sellers, verified listings, Pakistan-wide.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatChip(value = "1,200+", label = "Sellers")
                        StatChip(value = "8,400+", label = "Listings")
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier
                        .size(72.dp)
                        .shadow(6.dp, CircleShape)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Pets,
                            contentDescription = "Animals",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(value: String, label: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(text = value, style = MaterialTheme.typography.titleMedium)
            Text(text = label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun SectionHeader(title: String, action: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        if (action != null) {
            Text(
                text = action,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun CategoryPill(
    category: CategoryItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surface
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Surface(
        shape = MaterialTheme.shapes.large,
        color = containerColor,
        tonalElevation = if (selected) 4.dp else 3.dp,
        modifier = Modifier
            .width(128.dp)
            .height(56.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                }
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = category.name,
                    tint = if (selected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    },
                    modifier = Modifier
                        .padding(6.dp)
                        .size(18.dp)
                )
            }
            Text(
                text = category.name,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun CityFilter(
    selectedCity: String,
    onChange: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "City filter",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = "City",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = selectedCity,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            AssistChip(
                onClick = onChange,
                label = { Text(text = "Change") },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            )
        }
    }
}

@Composable
private fun ErrorBanner(
    message: String,
    onRetry: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@Composable
private fun EmptyStateCard(
    title: String,
    subtitle: String
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AdCard(
    ad: AdUiModel,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onOpen: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                if (ad.imageUrl != null) {
                    AsyncImage(
                        model = ad.imageUrl,
                        contentDescription = ad.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
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

                if (ad.featured) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .padding(12.dp)
                            .align(Alignment.TopStart)
                    ) {
                        Text(
                            text = "Featured",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text(
                        text = ad.category.uppercase(Locale.US),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = MaterialTheme.shapes.small
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = ad.price,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = ad.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (ad.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = ad.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (ad.details.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ad.details.take(3).forEach { detail ->
                            AssistChip(
                                onClick = {},
                                label = { Text(text = detail) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = "Location",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = ad.city,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Text(
                        text = ad.posted,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private data class CategoryItem(
    val name: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val filterValue: String = name
)

private data class AdUiModel(
    val id: String,
    val title: String,
    val price: String,
    val category: String,
    val city: String,
    val description: String,
    val details: List<String>,
    val posted: String,
    val featured: Boolean,
    val imageUrl: String?
)

private data class HomeFeedState(
    val isLoading: Boolean = false,
    val ads: List<AdUiModel> = emptyList(),
    val errorMessage: String? = null
)

private fun AdWithImage.toUiModel(): AdUiModel {
    val title = ad.title?.takeIf { it.isNotBlank() }
        ?: ad.breed?.takeIf { it.isNotBlank() }
        ?: "Premium listing"
    val category = ad.category?.takeIf { it.isNotBlank() } ?: "Animal"
    val city = ad.city?.takeIf { it.isNotBlank() } ?: "Pakistan"
    val description = ad.description?.trim().orEmpty()
    return AdUiModel(
        id = ad.id,
        title = title,
        price = formatPrice(ad.price),
        category = category,
        city = city,
        description = description,
        details = buildDetails(ad),
        posted = formatRelativeTime(ad.createdAt),
        featured = parseBoolean(ad.featured) ?: false,
        imageUrl = imageUrl
    )
}

private fun buildDetails(ad: com.halalclassified.app.data.ads.AdRecord): List<String> {
    val details = mutableListOf<String>()
    val genderValue = ad.gender?.trim().orEmpty()
    if (genderValue.isNotBlank()) {
        details.add(genderValue)
    }
    val ageValue = ad.age?.trim().orEmpty()
    if (ageValue.isNotBlank()) {
        val numericAge = ageValue.toDoubleOrNull()
        details.add(
            if (numericAge != null) {
                "${numericAge.toInt()} yrs"
            } else {
                ageValue
            }
        )
    }
    val weightValue = ad.weight?.trim().orEmpty()
    if (weightValue.isNotBlank()) {
        val numericWeight = weightValue.toDoubleOrNull()
        details.add(
            if (numericWeight != null) {
                "${numericWeight.toInt()} kg"
            } else {
                weightValue
            }
        )
    }
    val vaccinated = parseBoolean(ad.vaccinationStatus)
    if (vaccinated != null) {
        details.add(if (vaccinated) "Vaccinated" else "Not vaccinated")
    } else if (!ad.vaccinationStatus.isNullOrBlank()) {
        details.add(ad.vaccinationStatus.trim())
    }
    val delivery = parseBoolean(ad.deliveryAvailable)
    if (delivery != null) {
        details.add(if (delivery) "Delivery" else "Pickup")
    }
    return details
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

private fun parseBoolean(raw: String?): Boolean? {
    val value = raw?.trim()?.lowercase(Locale.US) ?: return null
    return when (value) {
        "true", "1", "yes", "y" -> true
        "false", "0", "no", "n" -> false
        else -> null
    }
}

private val categoryItems = AnimalCategoryOptions.map { option ->
    CategoryItem(option.label, Icons.Outlined.Pets, filterValue = option.filterValue)
}
