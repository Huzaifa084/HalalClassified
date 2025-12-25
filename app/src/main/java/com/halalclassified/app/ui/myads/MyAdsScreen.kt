package com.halalclassified.app.ui.myads

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PauseCircleOutline
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.halalclassified.app.data.ads.AdImageUpload
import com.halalclassified.app.data.ads.AdRecord
import com.halalclassified.app.data.ads.AdUpdate
import com.halalclassified.app.data.ads.AdWithImage
import com.halalclassified.app.data.ads.AdsRepository
import com.halalclassified.app.data.supabase.SupabaseClientProvider
import com.halalclassified.app.ui.common.AnimalCategoryOptions
import com.halalclassified.app.ui.common.BreedOptionsByCategory
import com.halalclassified.app.ui.common.CityPickerDialog
import com.halalclassified.app.ui.common.PakistanCities
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun MyAdsScreen(
    startEditAdId: String? = null,
    onEditConsumed: (() -> Unit)? = null
) {
    val supabase = SupabaseClientProvider.client
    val adsRepository = remember { AdsRepository(supabase) }
    val sessionStatus by supabase.auth.sessionStatus.collectAsState()
    val userId = (sessionStatus as? SessionStatus.Authenticated)?.session?.user?.id
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var feedState by remember { mutableStateOf(MyAdsState()) }
    var refreshKey by remember { mutableStateOf(0) }
    var selectedAdForEdit by remember { mutableStateOf<AdRecord?>(null) }
    var showCityPicker by remember { mutableStateOf(false) }
    var pendingCityChange by remember { mutableStateOf<(String) -> Unit>({}) }

    LaunchedEffect(userId, refreshKey) {
        if (userId.isNullOrBlank()) {
            feedState = feedState.copy(
                isLoading = false,
                errorMessage = "Log in to manage your listings."
            )
            return@LaunchedEffect
        }
        feedState = feedState.copy(isLoading = true, errorMessage = null)
        feedState = runCatching { adsRepository.fetchMyAds(userId) }
            .fold(
                onSuccess = { items ->
                    feedState.copy(isLoading = false, ads = items, errorMessage = null)
                },
                onFailure = { error ->
                    feedState.copy(
                        isLoading = false,
                        ads = emptyList(),
                        errorMessage = error.message ?: "Unable to load your ads."
                    )
                }
            )
    }

    LaunchedEffect(startEditAdId, feedState.ads, feedState.isLoading) {
        if (startEditAdId.isNullOrBlank()) return@LaunchedEffect
        if (feedState.isLoading) return@LaunchedEffect
        if (selectedAdForEdit != null) return@LaunchedEffect
        val match = feedState.ads.firstOrNull { it.ad.id == startEditAdId }?.ad
        if (match != null) {
            selectedAdForEdit = match
        }
        onEditConsumed?.invoke()
    }

    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "My Ads",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Edit, pause, or remove listings whenever you want.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (feedState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (feedState.errorMessage != null) {
                StatusBanner(message = feedState.errorMessage ?: "")
            }

            if (!feedState.isLoading && feedState.ads.isEmpty() && feedState.errorMessage == null) {
                StatusBanner(message = "No listings yet. Post your first ad from the Sell tab.")
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(feedState.ads, key = { it.ad.id }) { item ->
                    MyAdCard(
                        ad = item.ad,
                        imageUrl = item.imageUrl,
                        onToggle = { active ->
                            scope.launch {
                                val updated = adsRepository.setAdActive(item.ad.id, active)
                                if (updated != null) {
                                    feedState = feedState.copy(
                                        ads = feedState.ads.map {
                                            if (it.ad.id == item.ad.id) {
                                                it.copy(ad = updated)
                                            } else {
                                                it
                                            }
                                        }
                                    )
                                }
                            }
                        },
                        onEdit = { selectedAdForEdit = item.ad },
                        onDelete = {
                            scope.launch {
                                adsRepository.deleteAd(item.ad.id)
                                feedState = feedState.copy(
                                    ads = feedState.ads.filterNot { it.ad.id == item.ad.id }
                                )
                            }
                        }
                    )
                }
            }
        }
    }

    selectedAdForEdit?.let { ad ->
        EditAdDialog(
            ad = ad,
            onDismiss = { selectedAdForEdit = null },
            onRequestCityPicker = { callback ->
                pendingCityChange = callback
                showCityPicker = true
            },
            onSave = { update, newImages ->
                scope.launch {
                    val updated = adsRepository.updateAd(ad.id, update)
                    if (updated != null) {
                        feedState = feedState.copy(
                            ads = feedState.ads.map {
                                if (it.ad.id == ad.id) it.copy(ad = updated) else it
                            }
                        )
                    }
                    if (newImages.isNotEmpty()) {
                        val uploads = buildUploads(context, newImages)
                        adsRepository.replaceAdImages(ad.id, uploads)
                        val detail = adsRepository.fetchAdDetail(ad.id)
                        val cover = detail?.imageUrls?.firstOrNull()
                        if (cover != null) {
                            feedState = feedState.copy(
                                ads = feedState.ads.map {
                                    if (it.ad.id == ad.id) it.copy(imageUrl = cover) else it
                                }
                            )
                        }
                    }
                    selectedAdForEdit = null
                }
            }
        )
    }

    if (showCityPicker) {
        CityPickerDialog(
            cities = PakistanCities,
            selectedCity = "",
            includeAllPakistan = false,
            onSelect = {
                pendingCityChange(it)
                showCityPicker = false
            },
            onDismiss = { showCityPicker = false }
        )
    }
}

@Composable
private fun MyAdCard(
    ad: AdRecord,
    imageUrl: String?,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.secondaryContainer
                                )
                            )
                        )
                ) {
                    if (imageUrl != null) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = ad.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = ad.title?.takeIf { it.isNotBlank() }
                                ?: ad.breed?.takeIf { it.isNotBlank() }
                                ?: "Your listing",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = formatPrice(ad.price),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.LocationOn,
                                contentDescription = "City",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = ad.city?.takeIf { it.isNotBlank() } ?: "Pakistan",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    FilterChip(
                        selected = ad.isActive == true,
                        onClick = { onToggle(!(ad.isActive == true)) },
                        label = { Text(if (ad.isActive == true) "Active" else "Paused") }
                    )
                }
            }
            Divider(color = MaterialTheme.colorScheme.surfaceVariant)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onEdit) {
                    Icon(imageVector = Icons.Outlined.Edit, contentDescription = "Edit")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Edit")
                }
                TextButton(onClick = { onToggle(!(ad.isActive == true)) }) {
                    Icon(
                        imageVector = if (ad.isActive == true) {
                            Icons.Outlined.PauseCircleOutline
                        } else {
                            Icons.Outlined.PlayCircleOutline
                        },
                        contentDescription = "Toggle"
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (ad.isActive == true) "Disable" else "Enable")
                }
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(imageVector = Icons.Outlined.DeleteOutline, contentDescription = "Delete")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete")
                }
            }
        }
    }
}

@Composable
private fun EditAdDialog(
    ad: AdRecord,
    onDismiss: () -> Unit,
    onRequestCityPicker: ((String) -> Unit) -> Unit,
    onSave: (AdUpdate, List<Uri>) -> Unit
) {
    val supabase = SupabaseClientProvider.client
    val adsRepository = remember { AdsRepository(supabase) }
    var title by rememberSaveable { mutableStateOf(ad.title.orEmpty()) }
    var description by rememberSaveable { mutableStateOf(ad.description.orEmpty()) }
    var price by rememberSaveable { mutableStateOf(ad.price.orEmpty()) }
    var city by rememberSaveable { mutableStateOf(ad.city.orEmpty()) }
    var phone by rememberSaveable { mutableStateOf(ad.phone.orEmpty()) }
    var category by rememberSaveable { mutableStateOf(ad.category.orEmpty()) }
    var breed by rememberSaveable { mutableStateOf(ad.breed.orEmpty()) }
    var gender by rememberSaveable { mutableStateOf(ad.gender.orEmpty()) }
    var age by rememberSaveable { mutableStateOf(ad.age.orEmpty()) }
    var weight by rememberSaveable { mutableStateOf(ad.weight.orEmpty()) }
    var vaccinated by rememberSaveable { mutableStateOf(parseBoolean(ad.vaccinationStatus)) }
    var delivery by rememberSaveable { mutableStateOf(parseBoolean(ad.deliveryAvailable)) }
    var existingImages by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isLoadingImages by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedImages = uris
        }
    }

    LaunchedEffect(ad.id) {
        isLoadingImages = true
        existingImages = runCatching { adsRepository.fetchAdDetail(ad.id) }
            .getOrNull()
            ?.imageUrls
            .orEmpty()
        isLoadingImages = false
    }

    LaunchedEffect(category) {
        val options = BreedOptionsByCategory[category].orEmpty()
        if (options.isNotEmpty() && breed.isNotBlank() && breed !in options) {
            breed = ""
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit listing") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = "Photos", style = MaterialTheme.typography.titleSmall)
                Button(
                    onClick = { imagePicker.launch("image/*") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AddPhotoAlternate,
                        contentDescription = "Replace photos"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = if (selectedImages.isEmpty()) "Replace photos" else "Update photos")
                }
                if (isLoadingImages) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                } else if (selectedImages.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(selectedImages, key = { it.toString() }) { uri ->
                            PhotoPreview(uri = uri, onRemove = {
                                selectedImages = selectedImages.filterNot { it == uri }
                            })
                        }
                    }
                } else if (existingImages.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(existingImages, key = { it }) { url ->
                            PhotoPreviewUrl(url = url)
                        }
                    }
                }

                Divider()

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                DropdownField(
                    label = "Category",
                    selected = category,
                    options = AnimalCategoryOptions.map { it.label },
                    onSelect = { category = it }
                )
                DropdownField(
                    label = "Gender",
                    selected = gender,
                    options = listOf("Male", "Female"),
                    onSelect = { gender = it }
                )
                val breedOptions = BreedOptionsByCategory[category].orEmpty()
                if (breedOptions.isNotEmpty()) {
                    DropdownField(
                        label = "Breed",
                        selected = breed,
                        options = breedOptions,
                        onSelect = { breed = it }
                    )
                } else {
                    OutlinedTextField(
                        value = breed,
                        onValueChange = { breed = it },
                        label = { Text("Breed") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Price") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = city,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("City") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onRequestCityPicker { city = it }
                        }
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = age,
                    onValueChange = { age = it },
                    label = { Text("Age (years)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("Weight (kg)") },
                    modifier = Modifier.fillMaxWidth()
                )
                BooleanChoiceRow(
                    label = "Vaccinated",
                    value = vaccinated,
                    onChange = { vaccinated = it },
                    onClear = { vaccinated = null }
                )
                BooleanChoiceRow(
                    label = "Delivery available",
                    value = delivery,
                    onChange = { delivery = it },
                    onClear = { delivery = null }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val priceValue = parsePkrPrice(price)
                onSave(
                    AdUpdate(
                        title = title.trim().takeIf { it.isNotBlank() },
                        category = category.trim().takeIf { it.isNotBlank() },
                        gender = gender.trim().takeIf { it.isNotBlank() },
                        breed = breed.trim().takeIf { it.isNotBlank() },
                        description = description.trim().takeIf { it.isNotBlank() },
                        city = city.trim().takeIf { it.isNotBlank() },
                        phone = phone.trim().takeIf { it.isNotBlank() },
                        price = priceValue,
                        age = age.toDoubleOrNull(),
                        weight = weight.toDoubleOrNull(),
                        isVaccinated = vaccinated,
                        deliveryAvailable = delivery
                    ),
                    selectedImages
                )
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownField(
    label: String,
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun BooleanChoiceRow(
    label: String,
    value: Boolean?,
    onChange: (Boolean) -> Unit,
    onClear: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, style = MaterialTheme.typography.titleSmall)
            if (value != null) {
                TextButton(onClick = onClear) {
                    Text("Clear")
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilterChip(
                selected = value == true,
                onClick = { onChange(true) },
                label = { Text("Yes") }
            )
            FilterChip(
                selected = value == false,
                onClick = { onChange(false) },
                label = { Text("No") }
            )
        }
    }
}

@Composable
private fun PhotoPreview(uri: Uri, onRemove: () -> Unit) {
    Box {
        AsyncImage(
            model = uri,
            contentDescription = "Selected photo",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(96.dp)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.secondaryContainer
                        )
                    ),
                    shape = MaterialTheme.shapes.medium
                )
        )
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .padding(6.dp)
                .size(22.dp)
                .align(Alignment.TopEnd)
                .background(MaterialTheme.colorScheme.surface, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Remove photo",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun PhotoPreviewUrl(url: String) {
    AsyncImage(
        model = url,
        contentDescription = "Current photo",
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(96.dp)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.secondaryContainer
                    )
                ),
                shape = MaterialTheme.shapes.medium
            )
    )
}

private data class MyAdsState(
    val isLoading: Boolean = false,
    val ads: List<AdWithImage> = emptyList(),
    val errorMessage: String? = null
)

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

private fun parseBoolean(raw: String?): Boolean? {
    val value = raw?.trim()?.lowercase(Locale.US) ?: return null
    return when (value) {
        "true", "1", "yes", "y" -> true
        "false", "0", "no", "n" -> false
        else -> null
    }
}

private fun parsePkrPrice(raw: String): Long? {
    val trimmed = raw.trim()
    if (trimmed.isBlank()) return null
    val direct = trimmed.toLongOrNull()
    if (direct != null) return direct.takeIf { it > 0 }
    val parts = trimmed.split('.')
    if (parts.size == 2 && parts[1].isNotEmpty() && parts[1].all { it == '0' }) {
        val whole = parts[0].toLongOrNull() ?: return null
        return whole.takeIf { it > 0 }
    }
    return null
}

private fun buildUploads(context: Context, uris: List<Uri>): List<AdImageUpload> {
    return uris.mapNotNull { uri ->
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@mapNotNull null
        val mimeType = context.contentResolver.getType(uri)
        AdImageUpload(bytes = bytes, mimeType = mimeType)
    }
}
