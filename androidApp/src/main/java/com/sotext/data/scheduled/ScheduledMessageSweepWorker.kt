package com.sotext.data.scheduled

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sotext.domain.repository.ScheduledMessageRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Periodic safety net (registered every [SWEEP_INTERVAL_MINUTES] minutes in `PulseLinkApp`,
 * no network/battery constraints since it must run offline and on low battery too). Catches
 * anything the exact alarm missed: doze abuse, an OEM task-killer, an inexact-alarm fallback
 * drifting late, or a device reboot's [com.sotext.receiver.BootCompletedReceiver] re-arm racing
 * with this worker. Also does the crash-mid-send stale-PROCESSING reclaim, auto-retries
 * transiently failed sends, and sweeps orphaned attachment directories.
 */
@HiltWorker
class ScheduledMessageSweepWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repository: ScheduledMessageRepository,
    private val dispatcher: ScheduledMessageDispatcher
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val now = System.currentTimeMillis()

        // 1. Reclaim rows stuck in PROCESSING by a crash between claim and markSent/markFailed.
        repository.getStaleProcessing(now - STALE_PROCESSING_TIMEOUT_MS).forEach { stale ->
            repository.reclaimStale(stale.id)
        }

        // 2. Anything due that the alarm didn't fire for.
        repository.getDueForDispatch(now).forEach { due ->
            dispatcher.dispatch(due.id)
        }

        // 3. Auto-retry transient failures, backed off by retryCount * this worker's own interval.
        repository.getRetryableFailed(
            maxRetries = ScheduledMessageDispatcher.MAX_RETRY_COUNT,
            retryEligibleBeforeMillis = now - TimeUnit.MINUTES.toMillis(SWEEP_INTERVAL_MINUTES)
        ).forEach { failed ->
            dispatcher.dispatch(failed.id)
        }

        // 4. Orphaned attachment directories: guards against a missed cleanup call (e.g. a
        // force-kill between markSent and the dispatcher's own deleteRecursively()).
        sweepOrphanedAttachments()

        return Result.success()
    }

    private suspend fun sweepOrphanedAttachments() {
        val root = File(applicationContext.filesDir, "scheduled_attachments")
        val dirs = root.listFiles() ?: return
        // A dir may exist for a schedule the user is still filling out in ScheduleMessageSheet
        // (attachments are copied in at pick time, before Confirm/insert) - only sweep dirs whose
        // occurrenceKey doesn't belong to any still-active row, and give brand-new dirs a grace
        // period so an in-progress compose session isn't swept mid-edit.
        val activeOccurrenceKeys = repository.getAllActive().map { it.occurrenceKey }.toSet()
        val now = System.currentTimeMillis()
        dirs.forEach { dir ->
            val recentlyCreated = now - dir.lastModified() < ORPHAN_GRACE_PERIOD_MS
            if (dir.name !in activeOccurrenceKeys && !recentlyCreated) {
                dir.deleteRecursively()
            }
        }
    }

    companion object {
        const val SWEEP_INTERVAL_MINUTES = 15L
        private const val STALE_PROCESSING_TIMEOUT_MS = 5 * 60 * 1000L
        private const val ORPHAN_GRACE_PERIOD_MS = 60 * 60 * 1000L // 1 hour
    }
}
