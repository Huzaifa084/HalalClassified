package com.halalclassified.app.data.profile

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest

class ProfileRepository(
    private val supabase: SupabaseClient
) {
    suspend fun fetchProfile(userId: String): Profile? {
        val result = supabase.postgrest["profiles"].select {
            filter { eq("id", userId) }
            limit(1)
        }
        return result.decodeSingleOrNull()
    }

    suspend fun upsertProfile(profile: ProfileUpsert): Profile? {
        val result = supabase.postgrest["profiles"].upsert(
            value = profile,
            onConflict = "id"
        ) {
            select()
            limit(1)
        }
        return result.decodeSingleOrNull()
    }
}
