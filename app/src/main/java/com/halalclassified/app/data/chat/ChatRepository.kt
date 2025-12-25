package com.halalclassified.app.data.chat

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch

class ChatRepository(
    private val supabase: SupabaseClient
) {
    suspend fun getOrCreateChat(adId: String, buyerId: String, sellerId: String): ChatRecord {
        val existing = supabase.postgrest["chats"].select {
            filter {
                eq("ad_id", adId)
                eq("buyer_id", buyerId)
                eq("seller_id", sellerId)
            }
            limit(1)
        }.decodeSingleOrNull<ChatRecord>()
        if (existing != null) return existing

        val result = supabase.postgrest["chats"].insert(
            ChatInsert(adId = adId, buyerId = buyerId, sellerId = sellerId)
        ) {
            select()
            limit(1)
        }
        return result.decodeSingle()
    }

    suspend fun fetchChats(userId: String): List<ChatRecord> {
        val result = supabase.postgrest["chats"].select {
            filter {
                or {
                    eq("buyer_id", userId)
                    eq("seller_id", userId)
                }
            }
            order("created_at", Order.DESCENDING)
        }
        return result.decodeList()
    }

    suspend fun fetchMessages(chatId: String): List<MessageRecord> {
        val result = supabase.postgrest["messages"].select {
            filter { eq("chat_id", chatId) }
            order("created_at", Order.ASCENDING)
        }
        return result.decodeList()
    }

    suspend fun sendMessage(chatId: String, senderId: String, body: String): MessageRecord {
        val result = supabase.postgrest["messages"].insert(
            MessageInsert(chatId = chatId, senderId = senderId, body = body)
        ) {
            select()
            limit(1)
        }
        return result.decodeSingle()
    }

    fun observeMessages(chatId: String): Flow<MessageRecord> = channelFlow {
        val channel = supabase.realtime.channel("messages:$chatId")
        val changes = channel.postgresChangeFlow<PostgresAction.Insert>("public") {
            table = "messages"
            filter = "chat_id=eq.$chatId"
        }
        val job = launch {
            changes.collect { action ->
                val record = action.decodeRecord<MessageRecord>()
                trySend(record)
            }
        }
        channel.subscribe()
        awaitClose {
            job.cancel()
            launch { supabase.realtime.removeChannel(channel) }
        }
    }
}
