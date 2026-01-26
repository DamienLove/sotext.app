package com.RingerSong.free

import android.app.Application
import com.RingerSong.free.ads.AdServices
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class RingerSongApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AdServices.initialize(this)
    }
}
