package com.halalclassified.app.data.favorites

import com.halalclassified.app.data.ads.AdWithImage
import com.halalclassified.app.data.ads.AdsRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest

class FavoritesRepository(
    private val supabase: SupabaseClient,
    private val adsRepository: AdsRepository
) {
    suspend fun fetchFavoriteAdIds(userId: String): List<String> {
        val result = supabase.postgrest["favorites"].select {
            filter { eq("user_id", userId) }
        }
        return result.decodeList<FavoriteRecord>().map { it.adId }
    }

    suspend fun fetchFavorites(userId: String): List<AdWithImage> {
        val ids = fetchFavoriteAdIds(userId)
        return adsRepository.fetchAdsByIds(ids, activeOnly = true)
    }

    suspend fun toggleFavorite(userId: String, adId: String): Boolean {
        val existing = supabase.postgrest["favorites"].select {
            filter {
                eq("user_id", userId)
                eq("ad_id", adId)
            }
            limit(1)
        }.decodeSingleOrNull<FavoriteRecord>()
        return if (existing != null) {
            supabase.postgrest["favorites"].delete {
                filter {
                    eq("user_id", userId)
                    eq("ad_id", adId)
                }
            }
            false
        } else {
            supabase.postgrest["favorites"].insert(
                FavoriteInsert(userId = userId, adId = adId)
            )
            true
        }
    }
}
