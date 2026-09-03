package com.sotext.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sotext.data.scheduled.ScheduledMessageDispatcher
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Handles the "Retry" action on a failed-scheduled-send notification. */
@AndroidEntryPoint
class ScheduledMessageRetryReceiver : BroadcastReceiver() {

    @Inject lateinit var dispatcher: ScheduledMessageDispatcher

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(EXTRA_SCHEDULED_MESSAGE_ID, -1L)
        if (id <= 0L) return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                dispatcher.sendNowManually(id)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_SCHEDULED_MESSAGE_ID = "com.sotext.extra.SCHEDULED_MESSAGE_ID"
    }
}
