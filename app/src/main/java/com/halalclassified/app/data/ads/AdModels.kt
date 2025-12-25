package com.halalclassified.app.data.ads

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
data class AdRecord(
    val id: String,
    @SerialName("user_id")
    val userId: String? = null,
    val title: String? = null,
    @SerialName("price_pkr")
    @Serializable(with = FlexibleStringSerializer::class)
    val pricePkr: String? = null,
    @SerialName("price")
    @Serializable(with = FlexibleStringSerializer::class)
    val priceRaw: String? = null,
    val category: String? = null,
    val city: String? = null,
    val description: String? = null,
    val breed: String? = null,
    val gender: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    val age: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    val weight: String? = null,
    @JsonNames("is_vaccinated", "vaccinated", "vaccination_status")
    @Serializable(with = FlexibleStringSerializer::class)
    val vaccinationStatus: String? = null,
    @JsonNames("delivery_available", "delivery", "delivery_availability")
    @Serializable(with = FlexibleStringSerializer::class)
    val deliveryAvailable: String? = null,
    @JsonNames("is_featured", "featured")
    @Serializable(with = FlexibleStringSerializer::class)
    val featured: String? = null,
    val phone: String? = null,
    @SerialName("is_active")
    val isActive: Boolean? = null,
    @SerialName("created_at")
    val createdAt: String? = null
) {
    val price: String?
        get() = pricePkr?.takeIf { it.isNotBlank() } ?: priceRaw
}

@Serializable
data class AdImageRecord(
    val id: String? = null,
    @SerialName("ad_id")
    val adId: String? = null,
    @JsonNames("path", "image_path", "file_path")
    val path: String? = null,
    @JsonNames("image_url", "url")
    val imageUrl: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null
)

@Serializable
data class AdInsert(
    @SerialName("user_id")
    val userId: String,
    val title: String? = null,
    val category: String? = null,
    val city: String? = null,
    val description: String? = null,
    val breed: String? = null,
    val gender: String? = null,
    @SerialName("price_pkr")
    val price: Long? = null,
    val age: Double? = null,
    val weight: Double? = null,
    @SerialName("is_vaccinated")
    val isVaccinated: Boolean? = null,
    @SerialName("delivery_available")
    val deliveryAvailable: Boolean? = null,
    val phone: String? = null,
    @SerialName("is_active")
    val isActive: Boolean? = true
)

@Serializable
data class AdUpdate(
    val title: String? = null,
    val category: String? = null,
    val city: String? = null,
    val description: String? = null,
    val breed: String? = null,
    val gender: String? = null,
    @SerialName("price_pkr")
    val price: Long? = null,
    val age: Double? = null,
    val weight: Double? = null,
    @SerialName("is_vaccinated")
    val isVaccinated: Boolean? = null,
    @SerialName("delivery_available")
    val deliveryAvailable: Boolean? = null,
    val phone: String? = null,
    @SerialName("is_active")
    val isActive: Boolean? = null
)

@Serializable
data class AdImageInsert(
    @SerialName("ad_id")
    val adId: String,
    val path: String? = null,
    @SerialName("image_url")
    val imageUrl: String? = null
)

data class AdImageUpload(
    val bytes: ByteArray,
    val mimeType: String? = null
)

data class AdDetail(
    val ad: AdRecord,
    val imageUrls: List<String>
)
