package com.pulselink.beacon

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.MobileAds

class BeaconApp : Application() {
    private lateinit var appOpenManager: AppOpenAdManager

    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this)
        appOpenManager = AppOpenAdManager(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(appOpenManager)
    }
}
