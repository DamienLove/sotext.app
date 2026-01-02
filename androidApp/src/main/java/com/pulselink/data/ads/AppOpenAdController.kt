package com.pulselink.data.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppOpenAdController @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var appOpenAd: AppOpenAd? = null
    private var isLoading = false
    private var shouldShowAds = false
    private var hasShownThisSession = false
    private var lastShownAt: Long = 0L

    fun updateAvailability(showAds: Boolean) {
        val enabled = showAds && AdConfig.isAdsEnabled && AdConfig.appOpenUnitId.isNotBlank()
        shouldShowAds = enabled
        if (enabled) {
            loadIfNeeded()
        } else {
            clearAd()
        }
    }

    fun maybeShow(activity: Activity) {
        if (!shouldShowAds) return
        val now = System.currentTimeMillis()
        val tooSoon = hasShownThisSession && now - lastShownAt < MIN_INTERVAL_MS
        if (tooSoon) return
        val ad = appOpenAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    clearAd()
                    loadIfNeeded()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    clearAd()
                    loadIfNeeded()
                }
            }
            ad.show(activity)
            hasShownThisSession = true
            lastShownAt = now
        } else {
            loadIfNeeded()
        }
    }

    private fun loadIfNeeded() {
        if (!shouldShowAds || isLoading || appOpenAd != null) return
        isLoading = true
        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            context,
            AdConfig.appOpenUnitId,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    isLoading = false
                    appOpenAd = ad
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isLoading = false
                    appOpenAd = null
                }
            }
        )
    }

    private fun clearAd() {
        appOpenAd?.fullScreenContentCallback = null
        appOpenAd = null
        isLoading = false
    }

    companion object {
        // Cooldown to avoid blocking navigation with frequent full-screen ads  
        private const val MIN_INTERVAL_MS = 12 * 60 * 60 * 1000L // 12 hours   
    }
}
