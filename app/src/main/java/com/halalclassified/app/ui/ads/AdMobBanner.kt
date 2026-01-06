package com.halalclassified.app.ui.ads

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

@Composable
fun AdMobBanner(
    adUnitId: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    val widthDp = configuration.screenWidthDp
    val adSize = remember(widthDp) {
        AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp)
    }

    val adView = remember {
        AdView(context)
    }

    LaunchedEffect(adUnitId, adSize) {
        adView.setAdSize(adSize)
        adView.adUnitId = adUnitId
        adView.loadAd(AdRequest.Builder().build())
    }

    DisposableEffect(Unit) {
        onDispose {
            adView.destroy()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { adView }
    )
}
