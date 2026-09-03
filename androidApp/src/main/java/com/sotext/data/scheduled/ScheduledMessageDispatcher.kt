package com.sotext.data.scheduled

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import androidx.core.content.FileProvider
import com.sotext.data.sms.MessageNotificationManager
import com.sotext.data.sms.MmsSentReceiver
import com.sotext.data.sms.SmsSender
import com.sotext.domain.model.ScheduledAttachment
import com.sotext.domain.model.ScheduledMessage
import com.sotext.domain.repository.ScheduledMessageRepository
import com.sotext.util.normalizeSmsAddress
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

/**
 * The single place a [ScheduledMessage] actually gets sent, reached from three callers - the
 * alarm-triggered [ScheduledMessageSendWorker], the periodic [ScheduledMessageSweepWorker], and a
 * manual "Retry"/"Send now" tap - all funneling through [dispatch] so the double-send guard lives
 * in exactly one place.
 *
 * Reuses [SmsSender.sendSms]/[SmsSender.sendMms] exactly as `SmsThreadViewModel.sendMessage` and
 * `SmsRelayService.processMessage` already do - there is no separate Telephony-write path here.
 */
@Singleton
class ScheduledMessageDispatcher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: ScheduledMessageRepository,
    private val smsSender: SmsSender,
    private val alarmScheduler: ScheduledMessageAlarmScheduler
) {

    /**
     * Attempts to send [id]. Safe to call concurrently from multiple callers (alarm + sweep +
     * manual retry all racing) - only the caller that wins the Room CAS below actually sends.
     */
    suspend fun dispatch(id: Long) {
        if (!repository.claimForProcessing(id)) return // lost the race, or already terminal
        val message = repository.getById(id) ?: return

        val result = runCatching { sendNow(message) }
        val success = result.getOrDefault(false)
        val error = result.exceptionOrNull()?.message

        if (success) {
            repository.markSent(id, sentMessageId = null)
            cleanupAttachments(message)
            scheduleNextOccurrenceIfRecurring(message)
        } else {
            val reason = error ?: "SMS dispatch failed"
            repository.markFailed(id, reason)
            val updated = repository.getById(id)
            if (updated != null && updated.retryCount >= MAX_RETRY_COUNT) {
                cleanupAttachments(message)
            }
            MessageNotificationManager.notifyScheduledSendFailed(context, message.copy(lastError = reason))
        }
    }

    /** Manual "Send now": bypasses the alarm and dispatches immediately. */
    suspend fun sendNowManually(id: Long) = dispatch(id)

    private suspend fun sendNow(message: ScheduledMessage): Boolean = withContext(Dispatchers.IO) {
        val destinations = message.address.split(';', ',')
            .map { it.trim() }
            .filter { it.isNotBlank() }
        if (destinations.isEmpty()) return@withContext false

        var anySuccess = false
        destinations.forEach { dest ->
            val rawNumber = normalizeSmsAddress(dest)
            if (rawNumber.isBlank()) return@forEach
            val sent = if (message.attachments.isNotEmpty()) {
                sendScheduledMms(rawNumber, message.attachments)
            } else {
                smsSender.sendSms(rawNumber, message.body, awaitResult = true)
            }
            anySuccess = anySuccess || sent
        }
        anySuccess
    }

    /**
     * Sends each attachment as its own MMS, mirroring both `sendAttachmentViaSms` and
     * `SmsRelayService.downloadAndSendMms`: copy into `filesDir/exports`, wrap via the existing
     * FileProvider authority, grant the telephony stack read access, send, clean up via
     * [MmsSentReceiver] on completion.
     */
    private suspend fun sendScheduledMms(address: String, attachments: List<ScheduledAttachment>): Boolean {
        var anySuccess = false
        attachments.forEach { attachment ->
            val sourceFile = File(attachment.path)
            if (!sourceFile.exists()) return@forEach

            val exportDir = File(context.filesDir, "exports").apply { mkdirs() }
            val destFile = File(exportDir, "sched_${UUID.randomUUID()}_${sourceFile.name}")
            val copied = runCatching {
                sourceFile.inputStream().use { input ->
                    FileOutputStream(destFile).use { output -> input.copyTo(output) }
                }
            }.isSuccess
            if (!copied) return@forEach

            val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.export", destFile)
            listOf("com.android.mms.service", "com.android.providers.telephony", context.packageName).forEach { pkg ->
                runCatching { context.grantUriPermission(pkg, contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            }

            val cleanupIntent = Intent(context, MmsSentReceiver::class.java).apply {
                putExtra(MmsSentReceiver.EXTRA_FILE_PATH, destFile.absolutePath)
            }
            val pendingCleanup = PendingIntent.getBroadcast(
                context,
                destFile.name.hashCode(),
                cleanupIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val sent = try {
                smsSender.sendMms(address, contentUri, pendingCleanup)
            } catch (error: Exception) {
                Log.e(TAG, "Failed to send scheduled MMS attachment", error)
                false
            }
            anySuccess = anySuccess || sent
        }
        return anySuccess
    }

    private fun cleanupAttachments(message: ScheduledMessage) {
        if (message.attachments.isEmpty()) return
        val dir = scheduledAttachmentDir(context, message.occurrenceKey)
        runCatching { dir.deleteRecursively() }
    }

    private suspend fun scheduleNextOccurrenceIfRecurring(message: ScheduledMessage) {
        val rule = message.recurrenceRule ?: return
        val nextTime = RecurrenceCalculator.nextOccurrence(
            rule = rule,
            afterUtcMillis = message.scheduledForUtcMillis,
            zoneId = message.timezoneId
        ) ?: return

        // Belt-and-suspenders duplicate guard: if a crash/retry already spawned this occurrence
        // (e.g. dispatch() re-entered between markSent and here after a stale-PROCESSING reclaim),
        // don't spawn it twice. occurrenceKey also carries a unique Room index as the hard guarantee.
        if (repository.getBySeriesAndTime(message.seriesId, nextTime) != null) return

        val next = ScheduledMessage(
            seriesId = message.seriesId,
            occurrenceKey = UUID.randomUUID().toString(),
            threadId = message.threadId,
            address = message.address,
            body = message.body,
            attachments = emptyList(), // recurring series don't carry attachments forward (avoids re-copy/lifetime issues); user can re-attach by editing.
            lineId = message.lineId,
            scheduledForUtcMillis = nextTime,
            timezoneId = message.timezoneId,
            recurrenceRule = rule,
            status = com.sotext.domain.model.ScheduledMessageStatus.SCHEDULED
        )
        val newId = repository.insert(next)
        alarmScheduler.scheduleExact(next.copy(id = newId))
    }

    companion object {
        private const val TAG = "ScheduledMsgDispatcher"
        const val MAX_RETRY_COUNT = 3

        /**
         * Keyed by [ScheduledMessage.occurrenceKey], not the Room row id: a brand-new schedule's
         * attachments must be copied in at [ScheduleMessageSheet] confirm time, before the row
         * (and its autoincrement id) exists - occurrenceKey is generated client-side up front, so
         * it's the one stable identifier available at every point in this lifecycle.
         */
        fun scheduledAttachmentDir(context: Context, occurrenceKey: String): File =
            File(context.filesDir, "scheduled_attachments/$occurrenceKey")
    }
}
