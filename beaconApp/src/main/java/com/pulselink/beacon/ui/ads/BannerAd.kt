package com.pulselink.beacon.ui.ads

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.pulselink.beacon.BuildConfig

@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            MobileAds.initialize(context)
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = BuildConfig.AD_UNIT_BANNER
                loadAd(AdRequest.Builder().build())
            }
        },
        update = { view ->
            view.adUnitId = BuildConfig.AD_UNIT_BANNER
            if (view.adSize != AdSize.BANNER) view.setAdSize(AdSize.BANNER)
        }
    )
}
