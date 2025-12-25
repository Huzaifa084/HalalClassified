package com.halalclassified.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.halalclassified.app.ui.AppEntry
import com.halalclassified.app.ui.theme.HalalClassifiedTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HalalClassifiedTheme {
                AppEntry()
            }
        }
    }
}
