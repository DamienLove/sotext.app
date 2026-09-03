package com.sotext.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.sotext.data.scheduled.ScheduledMessageSendWorker
import dagger.hilt.android.AndroidEntryPoint

/**
 * Fired by [com.sotext.data.scheduled.ScheduledMessageAlarmScheduler]'s exact alarm. Deliberately
 * thin: it only hands off to an expedited one-time [ScheduledMessageSendWorker] rather than doing
 * the send itself via `goAsync()` - a receiver's `goAsync()` window is capped at roughly 10
 * seconds by the OS with no retry/constraint story, whereas WorkManager gives automatic backoff
 * and survives process death mid-send (the same [ScheduledMessageSendWorker] retry idiom already
 * used by `SmsSyncWorker`).
 *
 * The unique work name (`scheduled_send_<id>`) plus [ExistingWorkPolicy.KEEP] is a first
 * idempotency layer on top of the real guard - `ScheduledMessageDispatcher`'s Room
 * SCHEDULED->PROCESSING CAS - which is what actually prevents this alarm and the periodic sweep
 * worker from ever double-sending the same occurrence.
 */
@AndroidEntryPoint
class ScheduledMessageAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val messageId = intent.getLongExtra(EXTRA_SCHEDULED_MESSAGE_ID, -1L)
        if (messageId <= 0L) {
            Log.w(TAG, "Alarm fired with no valid scheduled message id")
            return
        }

        val request = OneTimeWorkRequestBuilder<ScheduledMessageSendWorker>()
            .setInputData(workDataOf(ScheduledMessageSendWorker.KEY_SCHEDULED_MESSAGE_ID to messageId))
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "scheduled_send_$messageId",
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    companion object {
        private const val TAG = "ScheduledMsgAlarmRcvr"
        const val EXTRA_SCHEDULED_MESSAGE_ID = "com.sotext.extra.SCHEDULED_MESSAGE_ID"
    }
}
