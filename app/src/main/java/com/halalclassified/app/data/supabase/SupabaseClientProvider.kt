package com.halalclassified.app.data.supabase

import com.halalclassified.app.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.ExternalAuthAction
import io.github.jan.supabase.gotrue.FlowType
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage

object SupabaseClientProvider {
    val client = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_KEY
    ) {
        install(Auth) {
            scheme = BuildConfig.SUPABASE_REDIRECT_SCHEME
            host = BuildConfig.SUPABASE_REDIRECT_HOST
            flowType = FlowType.IMPLICIT
            defaultExternalAuthAction = ExternalAuthAction.CustomTabs()
        }
        install(Postgrest)
        install(Storage)
        install(Realtime)
    }
}
