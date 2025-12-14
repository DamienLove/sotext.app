package com.pulselink.beacon

import android.app.Application
import com.google.android.gms.ads.MobileAds
import androidx.lifecycle.ProcessLifecycleOwner

class BeaconApp : Application() {
    private lateinit var appOpenManager: AppOpenAdManager

    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this)
        appOpenManager = AppOpenAdManager(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(appOpenManager)
    }
}
