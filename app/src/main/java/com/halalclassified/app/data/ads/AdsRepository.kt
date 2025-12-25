package com.halalclassified.app.data.ads

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import java.util.UUID

data class AdsQuery(
    val search: String = "",
    val category: String? = null,
    val city: String? = null,
    val limit: Long = 50
)

data class AdWithImage(
    val ad: AdRecord,
    val imageUrl: String?
)

class AdsRepository(
    private val supabase: SupabaseClient
) {
    suspend fun fetchFeed(query: AdsQuery): List<AdWithImage> {
        val ads = fetchAds(query)
        if (ads.isEmpty()) return emptyList()

        val adIds = ads.map { it.id }.distinct()
        val images = fetchAdImages(adIds)
        val imageByAdId = images
            .filter { it.adId != null }
            .groupBy { it.adId }
            .mapValues { (_, items) ->
                items.asSequence()
                    .mapNotNull { resolveImageUrl(it) }
                    .firstOrNull()
            }

        return ads.map { ad ->
            AdWithImage(ad = ad, imageUrl = imageByAdId[ad.id])
        }
    }

    suspend fun fetchMyAds(userId: String, limit: Long = 100): List<AdWithImage> {
        val result = supabase.postgrest["ads"].select {
            filter { eq("user_id", userId) }
            order("created_at", Order.DESCENDING)
            limit(limit)
        }
        val ads = result.decodeList<AdRecord>()
        if (ads.isEmpty()) return emptyList()

        val adIds = ads.map { it.id }.distinct()
        val images = fetchAdImages(adIds)
        val imageByAdId = images
            .filter { it.adId != null }
            .groupBy { it.adId }
            .mapValues { (_, items) ->
                items.asSequence()
                    .mapNotNull { resolveImageUrl(it) }
                    .firstOrNull()
            }

        return ads.map { ad ->
            AdWithImage(ad = ad, imageUrl = imageByAdId[ad.id])
        }
    }

    suspend fun fetchAdsByIds(adIds: List<String>, activeOnly: Boolean = true): List<AdWithImage> {
        if (adIds.isEmpty()) return emptyList()
        val result = supabase.postgrest["ads"].select {
            filter {
                isIn("id", adIds)
                if (activeOnly) {
                    eq("is_active", true)
                }
            }
            order("created_at", Order.DESCENDING)
        }
        val ads = result.decodeList<AdRecord>()
        if (ads.isEmpty()) return emptyList()

        val images = fetchAdImages(ads.map { it.id }.distinct())
        val imageByAdId = images
            .filter { it.adId != null }
            .groupBy { it.adId }
            .mapValues { (_, items) ->
                items.asSequence()
                    .mapNotNull { resolveImageUrl(it) }
                    .firstOrNull()
            }

        return ads.map { ad ->
            AdWithImage(ad = ad, imageUrl = imageByAdId[ad.id])
        }
    }

    suspend fun fetchAdDetail(adId: String): AdDetail? {
        val result = supabase.postgrest["ads"].select {
            filter { eq("id", adId) }
            limit(1)
        }
        val ad = result.decodeSingleOrNull<AdRecord>() ?: return null
        val images = fetchAdImages(listOf(adId))
            .mapNotNull { resolveImageUrl(it) }
        return AdDetail(ad = ad, imageUrls = images)
    }

    suspend fun createAd(ad: AdInsert, images: List<AdImageUpload>): AdRecord {
        val created = supabase.postgrest["ads"].insert(ad) {
            select()
            limit(1)
        }.decodeSingle<AdRecord>()

        if (images.isNotEmpty()) {
            uploadAdImages(created.id, images)
        }
        return created
    }

    suspend fun updateAd(adId: String, update: AdUpdate): AdRecord? {
        val result = supabase.postgrest["ads"].update(update) {
            filter { eq("id", adId) }
            select()
            limit(1)
        }
        return result.decodeSingleOrNull()
    }

    suspend fun replaceAdImages(adId: String, images: List<AdImageUpload>) {
        val existing = fetchAdImages(listOf(adId))
        val paths = existing.mapNotNull { extractStoragePath(it) }
        if (paths.isNotEmpty()) {
            supabase.storage.from("ad-images").delete(paths)
        }
        supabase.postgrest["ad_images"].delete {
            filter { eq("ad_id", adId) }
        }
        if (images.isNotEmpty()) {
            uploadAdImages(adId, images)
        }
    }

    suspend fun setAdActive(adId: String, active: Boolean): AdRecord? {
        return updateAd(adId, AdUpdate(isActive = active))
    }

    suspend fun deleteAd(adId: String) {
        val images = fetchAdImages(listOf(adId))
        val paths = images.mapNotNull { extractStoragePath(it) }
        if (paths.isNotEmpty()) {
            supabase.storage.from("ad-images").delete(paths)
        }
        supabase.postgrest["ad_images"].delete {
            filter { eq("ad_id", adId) }
        }
        supabase.postgrest["ads"].delete {
            filter { eq("id", adId) }
        }
    }

    private suspend fun fetchAds(query: AdsQuery): List<AdRecord> {
        val searchValue = query.search.trim()
        val categoryValue = query.category?.trim().orEmpty()
        val cityValue = query.city?.trim().orEmpty()

        val result = supabase.postgrest["ads"].select {
            filter {
                eq("is_active", true)
                if (categoryValue.isNotBlank()) {
                    ilike("category", "%$categoryValue%")
                }
                if (cityValue.isNotBlank()) {
                    ilike("city", cityValue)
                }
                if (searchValue.isNotBlank()) {
                    or {
                        ilike("title", "%$searchValue%")
                        ilike("description", "%$searchValue%")
                        ilike("breed", "%$searchValue%")
                    }
                }
            }
            order("created_at", Order.DESCENDING)
            limit(query.limit)
        }
        return result.decodeList()
    }

    private suspend fun fetchAdImages(adIds: List<String>): List<AdImageRecord> {
        if (adIds.isEmpty()) return emptyList()
        val result = supabase.postgrest["ad_images"].select {
            filter { isIn("ad_id", adIds) }
        }
        return result.decodeList()
    }

    private suspend fun uploadAdImages(adId: String, images: List<AdImageUpload>) {
        val bucket = supabase.storage.from("ad-images")
        val imageRows = images.map { upload ->
            val extension = when (upload.mimeType?.lowercase()) {
                "image/png" -> "png"
                "image/webp" -> "webp"
                "image/jpeg", "image/jpg" -> "jpg"
                else -> "jpg"
            }
            val path = "$adId/${UUID.randomUUID()}.$extension"
            bucket.upload(path, upload.bytes)
            AdImageInsert(adId = adId, path = path)
        }
        if (imageRows.isNotEmpty()) {
            supabase.postgrest["ad_images"].insert(imageRows)
        }
    }

    private fun resolveImageUrl(image: AdImageRecord): String? {
        val raw = image.imageUrl?.trim().takeIf { !it.isNullOrBlank() }
            ?: image.path?.trim().takeIf { !it.isNullOrBlank() }
            ?: return null
        if (raw.startsWith("http://") || raw.startsWith("https://")) {
            return raw
        }
        val normalized = raw.trimStart('/')
        val bucketPrefix = "ad-images/"
        val path = if (normalized.startsWith(bucketPrefix)) {
            normalized.removePrefix(bucketPrefix)
        } else {
            normalized
        }
        return supabase.storage.from("ad-images").publicUrl(path)
    }

    private fun extractStoragePath(image: AdImageRecord): String? {
        val raw = image.path?.trim().takeIf { !it.isNullOrBlank() }
            ?: image.imageUrl?.trim().takeIf { !it.isNullOrBlank() }
            ?: return null
        if (raw.startsWith("http://") || raw.startsWith("https://")) {
            if (raw.contains("/ad-images/")) {
                return raw.substringAfter("/ad-images/").trimStart('/')
            }
            return null
        }
        val normalized = raw.trimStart('/')
        val bucketPrefix = "ad-images/"
        return if (normalized.startsWith(bucketPrefix)) {
            normalized.removePrefix(bucketPrefix)
        } else {
            normalized
        }
    }
}
