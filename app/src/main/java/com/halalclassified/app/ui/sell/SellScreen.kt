package com.halalclassified.app.ui.sell

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.interaction.MutableInteractionSource
import coil.compose.AsyncImage
import com.halalclassified.app.data.ads.AdImageUpload
import com.halalclassified.app.data.ads.AdInsert
import com.halalclassified.app.data.ads.AdsRepository
import com.halalclassified.app.data.supabase.SupabaseClientProvider
import com.halalclassified.app.ui.common.AnimalCategoryOptions
import com.halalclassified.app.ui.common.BreedOptionsByCategory
import com.halalclassified.app.ui.common.CityPickerDialog
import com.halalclassified.app.ui.common.PakistanCities
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.launch

@Composable
fun SellScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val supabase = SupabaseClientProvider.client
    val adsRepository = remember { AdsRepository(supabase) }
    val sessionStatus by supabase.auth.sessionStatus.collectAsState()
    val userId = (sessionStatus as? SessionStatus.Authenticated)?.session?.user?.id

    var selectedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var title by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("") }
    var gender by rememberSaveable { mutableStateOf("") }
    var breed by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }
    var city by rememberSaveable { mutableStateOf("") }
    var age by rememberSaveable { mutableStateOf("") }
    var weight by rememberSaveable { mutableStateOf("") }
    var vaccinated by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var delivery by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var successMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var showCityPicker by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedImages = uris
        }
    }

    LaunchedEffect(category) {
        val options = BreedOptionsByCategory[category].orEmpty()
        if (options.isNotEmpty() && breed.isNotBlank() && breed !in options) {
            breed = ""
        }
    }

    LaunchedEffect(errorMessage, successMessage) {
        if (!errorMessage.isNullOrBlank() || !successMessage.isNullOrBlank()) {
            scrollState.animateScrollTo(0)
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Post an Ad",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Share clear details so buyers can reach you quickly.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (errorMessage != null) {
                FormBanner(message = errorMessage ?: "", isError = true)
            } else if (successMessage != null) {
                FormBanner(message = successMessage ?: "", isError = false)
            }

            FormSection(title = "Photos (required)") {
                Button(
                    onClick = { imagePicker.launch("image/*") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AddPhotoAlternate,
                        contentDescription = "Add photos"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = if (selectedImages.isEmpty()) "Add photos" else "Replace photos")
                }
                if (selectedImages.isNotEmpty()) {
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
                }
            }

            FormSection(title = "Animal details") {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Listing title (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                DropdownField(
                    label = "Category *",
                    selected = category,
                    options = AnimalCategoryOptions.map { it.label },
                    onSelect = { category = it }
                )

                DropdownField(
                    label = "Gender *",
                    selected = gender,
                    options = listOf("Male", "Female"),
                    onSelect = { gender = it }
                )

                val breedOptions = BreedOptionsByCategory[category].orEmpty()
                if (breedOptions.isNotEmpty()) {
                    DropdownField(
                        label = "Breed *",
                        selected = breed,
                        options = breedOptions,
                        onSelect = { breed = it }
                    )
                } else {
                    OutlinedTextField(
                        value = breed,
                        onValueChange = { breed = it },
                        label = { Text("Breed *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description *") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            FormSection(title = "Pricing & location") {
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Price (PKR) *") },
                    modifier = Modifier.fillMaxWidth()
                )

                val cityInteraction = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = cityInteraction,
                            indication = null,
                            role = Role.Button
                        ) { showCityPicker = true }
                ) {
                    OutlinedTextField(
                        value = city,
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        label = { Text("City *") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.LocationOn,
                                contentDescription = "City"
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            disabledContainerColor = MaterialTheme.colorScheme.surface,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledLeadingIconColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            FormSection(title = "Contact") {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone number *") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            FormSection(title = "Optional details") {
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

            Divider()

            Button(
                onClick = {
                    errorMessage = null
                    successMessage = null
                    val priceValue = parsePkrPrice(price)
                    val validationError = validateForm(
                        userId = userId,
                        images = selectedImages,
                        category = category,
                        gender = gender,
                        breed = breed,
                        description = description,
                        phone = phone,
                        priceValue = priceValue,
                        city = city
                    )
                    if (validationError != null) {
                        errorMessage = validationError
                        return@Button
                    }
                    scope.launch {
                        isLoading = true
                        val uploads = buildUploads(context, selectedImages)
                        runCatching {
                            adsRepository.createAd(
                                AdInsert(
                                    userId = userId ?: "",
                                    title = title.trim().takeIf { it.isNotBlank() },
                                    category = category,
                                    city = city,
                                    description = description.trim(),
                                    breed = breed,
                                    gender = gender,
                                    price = priceValue,
                                    age = age.toDoubleOrNull(),
                                    weight = weight.toDoubleOrNull(),
                                    isVaccinated = vaccinated,
                                    deliveryAvailable = delivery,
                                    phone = phone.trim(),
                                    isActive = true
                                ),
                                uploads
                            )
                        }.onSuccess {
                            successMessage = "Your ad is live. Buyers can reach you now."
                            title = ""
                            category = ""
                            gender = ""
                            breed = ""
                            description = ""
                            phone = ""
                            price = ""
                            city = ""
                            age = ""
                            weight = ""
                            vaccinated = null
                            delivery = null
                            selectedImages = emptyList()
                        }.onFailure { error ->
                            errorMessage = error.message ?: "Unable to post your ad."
                        }
                        isLoading = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                Text(text = "Publish ad")
            }
        }
    }

    if (showCityPicker) {
        CityPickerDialog(
            cities = PakistanCities,
            selectedCity = city,
            includeAllPakistan = false,
            onSelect = {
                city = it
                showCityPicker = false
            },
            onDismiss = { showCityPicker = false }
        )
    }
}

@Composable
private fun FormSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        content()
    }
}

@Composable
private fun FormBanner(message: String, isError: Boolean) {
    val background = if (isError) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val content = if (isError) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }
    Surface(
        color = background,
        contentColor = content,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
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

private fun validateForm(
    userId: String?,
    images: List<Uri>,
    category: String,
    gender: String,
    breed: String,
    description: String,
    phone: String,
    priceValue: Long?,
    city: String
): String? {
    if (userId.isNullOrBlank()) return "You need to be logged in to post an ad."
    if (images.isEmpty()) return "Please add at least one photo."
    if (category.isBlank()) return "Select a category."
    if (gender.isBlank()) return "Select a gender."
    if (breed.isBlank()) return "Select a breed."
    if (description.isBlank()) return "Add a short description."
    if (phone.isBlank()) return "Enter your phone number."
    if (priceValue == null) return "Enter a valid price."
    if (city.isBlank()) return "Select a city."
    return null
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
