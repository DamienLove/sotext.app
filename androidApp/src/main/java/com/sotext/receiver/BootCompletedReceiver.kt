package com.sotext.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.sotext.data.scheduled.ScheduledMessageAlarmScheduler
import com.sotext.domain.repository.ScheduledMessageRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * `AlarmManager` alarms do not survive a reboot, so every still-[SCHEDULED][com.sotext.domain.model.ScheduledMessageStatus.SCHEDULED]
 * row needs its exact alarm re-armed once the device (and this app) comes back up.
 * `RECEIVE_BOOT_COMPLETED` was already declared for other features; this is its first receiver.
 */
@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {

    @Inject lateinit var scheduledMessageRepository: ScheduledMessageRepository
    @Inject lateinit var alarmScheduler: ScheduledMessageAlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val scheduled = scheduledMessageRepository.getAllScheduled()
                scheduled.forEach { alarmScheduler.scheduleExact(it) }
                Log.d(TAG, "Re-armed ${scheduled.size} scheduled message alarm(s) after boot")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to re-arm scheduled message alarms after boot", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "BootCompletedReceiver"
    }
}
