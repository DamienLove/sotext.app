package com.sotext.data.scheduled

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.sotext.domain.model.ScheduledMessage
import com.sotext.receiver.ScheduledMessageAlarmReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Arms/cancels the exact-time OS alarm that fires a [ScheduledMessage]'s send. This is the
 * primary dispatch trigger; [ScheduledMessageSweepWorker] is the periodic fallback for anything
 * this misses (doze abuse, OEM task-killers, a missed [android.content.Intent.ACTION_BOOT_COMPLETED]).
 *
 * No `AlarmManager` usage existed anywhere in this codebase before this feature - this is the
 * first.
 */
@Singleton
class ScheduledMessageAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val alarmManager: AlarmManager?
        get() = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    fun scheduleExact(message: ScheduledMessage) {
        val manager = alarmManager ?: return
        val pendingIntent = buildPendingIntent(message.id)

        val canBeExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()
        try {
            if (canBeExact) {
                manager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    message.scheduledForUtcMillis,
                    pendingIntent
                )
            } else {
                // Exact-alarm permission not granted (Android 12+): fall back to an inexact,
                // doze-deferrable alarm. ScheduledMessageSweepWorker's 15-minute sweep bounds
                // worst-case lateness from this fallback.
                manager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    message.scheduledForUtcMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Unable to schedule exact alarm for ${message.id}, falling back to sweep only", e)
        }
    }

    fun cancel(messageId: Long) {
        val manager = alarmManager ?: return
        manager.cancel(buildPendingIntent(messageId))
    }

    fun canScheduleExactAlarms(): Boolean {
        val manager = alarmManager ?: return false
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()
    }

    private fun buildPendingIntent(messageId: Long): PendingIntent {
        val intent = Intent(context, ScheduledMessageAlarmReceiver::class.java).apply {
            putExtra(ScheduledMessageAlarmReceiver.EXTRA_SCHEDULED_MESSAGE_ID, messageId)
        }
        return PendingIntent.getBroadcast(
            context,
            messageId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        private const val TAG = "ScheduledMsgAlarms"
    }
}
