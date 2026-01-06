package com.halalclassified.app

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.halalclassified.app.data.supabase.SupabaseClientProvider
import com.halalclassified.app.ui.AppEntry
import com.halalclassified.app.ui.theme.HalalClassifiedTheme
import io.github.jan.supabase.gotrue.handleDeeplinks

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        SupabaseClientProvider.client.handleDeeplinks(intent)
        setContent {
            HalalClassifiedTheme {
                AppEntry()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        SupabaseClientProvider.client.handleDeeplinks(intent)
    }
}
