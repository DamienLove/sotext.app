package com.sotext

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.initialization.InitializationStatus
import com.google.firebase.FirebaseApp
import com.sotext.auth.FirebaseAuthManager
import com.sotext.data.ads.AdConfig
import com.sotext.data.ads.AppOpenAdController
import com.sotext.assistant.AssistantShortcuts
import com.sotext.data.remoteconfig.RemoteConfigService
import com.sotext.data.sms.SmsRelayService
import com.sotext.data.sms.SmsSyncManager
import com.sotext.data.sms.OtpCleanupWorker
import com.sotext.data.scheduled.ScheduledMessageAlarmScheduler
import com.sotext.data.scheduled.ScheduledMessageSweepWorker
import com.sotext.data.scheduled.ScheduledMessageSyncService
import com.sotext.domain.repository.ScheduledMessageRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class PulseLinkApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var appOpenAdController: AppOpenAdController
    @Inject lateinit var firebaseAuthManager: FirebaseAuthManager
    @Inject lateinit var remoteConfigService: RemoteConfigService
    @Inject lateinit var smsRelayService: SmsRelayService
    @Inject lateinit var smsSyncManager: SmsSyncManager
    @Inject lateinit var scheduledMessageRepository: ScheduledMessageRepository
    @Inject lateinit var scheduledMessageAlarmScheduler: ScheduledMessageAlarmScheduler
    @Inject lateinit var scheduledMessageSyncService: ScheduledMessageSyncService

    override val workManagerConfiguration: Configuration by lazy {
        Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        // Touch auth early so listeners register before any UI state checks
        firebaseAuthManager.currentUser()
        smsRelayService.start()
        smsSyncManager.start()
        scheduledMessageSyncService.start()
        CoroutineScope(Dispatchers.IO).launch {
            remoteConfigService.fetchAndActivate()
        }
        AssistantShortcuts.publish(this)
        if (AdConfig.isAdsEnabled) {
            Thread {
                MobileAds.initialize(this) { initializationStatus ->
                    logAdapterStatus(initializationStatus)
                }
            }.start()
            appOpenAdController.updateAvailability(false)
        }

        // SMS sync is now triggered on-demand when messages are sent/received
        // No need for periodic sync

        val otpCleanupRequest = PeriodicWorkRequest.Builder(
            OtpCleanupWorker::class.java,
            24,
            TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "OtpCleanup",
            ExistingPeriodicWorkPolicy.KEEP,
            otpCleanupRequest
        )

        val scheduledMessageSweepRequest = PeriodicWorkRequest.Builder(
            ScheduledMessageSweepWorker::class.java,
            ScheduledMessageSweepWorker.SWEEP_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        ).setConstraints(Constraints.Builder().build()).build() // must run offline/low-battery: exact-time firing is time-critical

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "ScheduledMessageSweep",
            ExistingPeriodicWorkPolicy.KEEP,
            scheduledMessageSweepRequest
        )

        // Cold start (process death, not just reboot - BootCompletedReceiver only covers reboots)
        // is a good moment to make sure every SCHEDULED row still has a live alarm.
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                scheduledMessageRepository.getAllScheduled().forEach { scheduledMessageAlarmScheduler.scheduleExact(it) }
            }
        }
    }

    private fun logAdapterStatus(initializationStatus: InitializationStatus) {
        val statusMap = initializationStatus.adapterStatusMap
        for ((adapterClass, status) in statusMap) {
            Log.d(
                "AdMob",
                "Adapter: $adapterClass, Status: ${status.description}, Latency: ${status.latency}"
            )
        }
    }
}
