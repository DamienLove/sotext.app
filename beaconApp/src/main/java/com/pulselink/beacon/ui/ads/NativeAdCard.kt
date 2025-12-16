package com.pulselink.beacon.ui.ads

import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.pulselink.beacon.BuildConfig

@Composable
fun NativeAdCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val nativeAdState = remember { mutableStateOf<NativeAd?>(null) }

    DisposableEffect(Unit) {
        onDispose { nativeAdState.value?.destroy() }
    }

    LaunchedEffect(Unit) {
        val loader = AdLoader.Builder(context, BuildConfig.AD_UNIT_NATIVE)
            .forNativeAd { ad ->
                nativeAdState.value?.destroy()
                nativeAdState.value = ad
            }
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setRequestMultipleImages(false)
                    .build()
            )
            .build()
        loader.loadAd(AdRequest.Builder().build())
    }

    val ad = nativeAdState.value ?: return

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val adView = NativeAdView(ctx)
            val container = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16, 12, 16, 12)
            }
            val row = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                weightSum = 1f
            }
            val icon = ImageView(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(56, 56)
            }
            val headline = TextView(ctx).apply { textSize = 16f }
            row.addView(icon)
            row.addView(headline)

            val body = TextView(ctx).apply { textSize = 14f }
            val media = MediaView(ctx)

            container.addView(row)
            container.addView(media, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 200))
            container.addView(body)

            adView.headlineView = headline
            adView.bodyView = body
            adView.mediaView = media
            adView.iconView = icon
            adView.addView(container)
            adView
        },
        update = { adView ->
            (adView.headlineView as? TextView)?.text = ad.headline
            (adView.bodyView as? TextView)?.text = ad.body ?: ""
            (adView.iconView as? ImageView)?.setImageDrawable(ad.icon?.drawable)
            adView.setNativeAd(ad)
        }
    )
}
