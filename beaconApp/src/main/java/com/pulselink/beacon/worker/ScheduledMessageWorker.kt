package com.pulselink.beacon.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pulselink.beacon.data.SmsRepository
import com.pulselink.beacon.data.scheduled.BeaconDatabase
import com.pulselink.beacon.data.scheduled.MessageStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ScheduledMessageWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val database = BeaconDatabase.getDatabase(applicationContext)
        val dao = database.scheduledMessageDao()
        val smsRepository = SmsRepository(applicationContext)

        val messageId = inputData.getLong("messageId", -1L)

        if (messageId != -1L) {
            // Process specific message (prevents race conditions)
            val message = dao.getById(messageId)
            if (message != null && message.status == MessageStatus.PENDING) {
                // Double check time just in case worker ran early (though delay should prevent this)
                // But generally safe to send if user triggered it via logic that calculated the delay.
                // However, let's just send it.

                try {
                    val success = smsRepository.sendSms(message.address, message.body)
                    if (success) {
                        dao.update(message.copy(status = MessageStatus.SENT))
                    } else {
                        dao.update(message.copy(status = MessageStatus.FAILED))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    dao.update(message.copy(status = MessageStatus.FAILED))
                }
            }
        } else {
            // Fallback for generic periodic checks (if we add them later)
            // For now, return success.
        }

        Result.success()
    }
}
