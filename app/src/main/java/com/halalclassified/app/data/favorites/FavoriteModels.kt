package com.halalclassified.app.data.favorites

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FavoriteRecord(
    val id: String? = null,
    @SerialName("user_id")
    val userId: String,
    @SerialName("ad_id")
    val adId: String,
    @SerialName("created_at")
    val createdAt: String? = null
)

@Serializable
data class FavoriteInsert(
    @SerialName("user_id")
    val userId: String,
    @SerialName("ad_id")
    val adId: String
)
