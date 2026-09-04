package com.sotext.data.scheduled

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Handed off from [com.sotext.receiver.ScheduledMessageAlarmReceiver] when the exact alarm fires.
 * All the actual work - including the double-send guard - lives in [ScheduledMessageDispatcher];
 * this is just the WorkManager wrapper that gets retry/backoff and survival across process death,
 * same shape as the existing `OtpCleanupWorker`.
 */
@HiltWorker
class ScheduledMessageSendWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val dispatcher: ScheduledMessageDispatcher
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val id = inputData.getLong(KEY_SCHEDULED_MESSAGE_ID, -1L)
        if (id <= 0L) return Result.failure()

        return try {
            dispatcher.dispatch(id)
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val KEY_SCHEDULED_MESSAGE_ID = "scheduled_message_id"
        private const val MAX_ATTEMPTS = 3
    }
}
