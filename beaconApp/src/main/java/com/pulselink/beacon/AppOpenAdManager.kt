package com.pulselink.beacon

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.appopen.AppOpenAd.AppOpenAdLoadCallback
import com.google.android.gms.ads.FullScreenContentCallback
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Minimal app-open ad loader; for free flavor only (Beacon app).
 */
class AppOpenAdManager(private val app: Application) : DefaultLifecycleObserver, Application.ActivityLifecycleCallbacks {
    private var appOpenAd: AppOpenAd? = null
    private var isShowing = AtomicBoolean(false)
    private var currentActivity: Activity? = null

    init {
        app.registerActivityLifecycleCallbacks(this)
        loadAd()
    }

    override fun onStart(owner: LifecycleOwner) {
        showAdIfAvailable()
    }

    private fun loadAd() {
        if (appOpenAd != null) return
        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            app,
            BuildConfig.AD_UNIT_APP_OPEN,
            request,
            AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
            object : AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                }
            }
        )
    }

    private fun showAdIfAvailable() {
        val activity = currentActivity ?: return
        if (isShowing.get()) return
        appOpenAd?.let { ad ->
            isShowing.set(true)
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    appOpenAd = null
                    isShowing.set(false)
                    loadAd()
                }
            }
            ad.show(activity)
        } ?: loadAd()
    }

    override fun onActivityStarted(activity: Activity) {
        currentActivity = activity
    }
    override fun onActivityResumed(activity: Activity) { currentActivity = activity }
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivityDestroyed(activity: Activity) {}
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
}
