package com.halalclassified.app.ui.ads

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.LoadAdError
import android.view.ViewGroup

@Composable
fun AdMobBanner(
    adUnitId: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val widthDp = configuration.screenWidthDp
    val adSize = remember(widthDp) {
        AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp)
    }

    val adHeightDp = remember(adSize, density) {
        with(density) { adSize.getHeightInPixels(context).toDp() }
    }

    val adView = remember {
        AdView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                adSize.getHeightInPixels(context)
            )
        }
    }

    LaunchedEffect(adUnitId, adSize) {
        Log.i(TAG, "Loading banner. unitId=$adUnitId widthDp=$widthDp size=${adSize.width}x${adSize.height}")
        adView.setAdSize(adSize)
        adView.adUnitId = adUnitId

        adView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                Log.i(TAG, "Banner loaded")
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                Log.e(TAG, "Banner failed to load: code=${error.code} domain=${error.domain} message=${error.message}")
            }

            override fun onAdImpression() {
                Log.i(TAG, "Banner impression")
            }

            override fun onAdClicked() {
                Log.i(TAG, "Banner clicked")
            }
        }

        adView.loadAd(AdRequest.Builder().build())
    }

    DisposableEffect(Unit) {
        onDispose {
            adView.destroy()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { adView },
        update = {
            // Keep layout params in sync when orientation/width changes.
            it.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                adSize.getHeightInPixels(context)
            )
        }
    )
}

private const val TAG = "AdMobBanner"
