package com.halalclassified.app.data.chat

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatRecord(
    val id: String,
    @SerialName("ad_id")
    val adId: String,
    @SerialName("buyer_id")
    val buyerId: String,
    @SerialName("seller_id")
    val sellerId: String,
    @SerialName("created_at")
    val createdAt: String? = null
)

@Serializable
data class ChatInsert(
    @SerialName("ad_id")
    val adId: String,
    @SerialName("buyer_id")
    val buyerId: String,
    @SerialName("seller_id")
    val sellerId: String
)

@Serializable
data class MessageRecord(
    val id: String,
    @SerialName("chat_id")
    val chatId: String,
    @SerialName("sender_id")
    val senderId: String,
    val body: String,
    @SerialName("created_at")
    val createdAt: String? = null
)

@Serializable
data class MessageInsert(
    @SerialName("chat_id")
    val chatId: String,
    @SerialName("sender_id")
    val senderId: String,
    val body: String
)
