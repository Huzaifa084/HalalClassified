package com.halalclassified.app.ads

import android.content.Context
import androidx.startup.Initializer
import com.google.android.gms.ads.MobileAds
import android.util.Log

class AdMobInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        MobileAds.initialize(context) { status ->
            Log.i(TAG, "MobileAds initialized: ${status.adapterStatusMap.keys}")
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()

    private companion object {
        const val TAG = "AdMobInit"
    }
}
