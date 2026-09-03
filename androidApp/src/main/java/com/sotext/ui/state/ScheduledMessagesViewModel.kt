package com.sotext.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sotext.data.scheduled.ScheduledMessageAlarmScheduler
import com.sotext.data.scheduled.ScheduledMessageDispatcher
import com.sotext.domain.model.RecurrenceRule
import com.sotext.domain.model.ScheduledMessage
import com.sotext.domain.repository.ScheduledMessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Backs the Scheduled hub - every upcoming/failed scheduled message across every conversation, grouped by date. */
@HiltViewModel
class ScheduledMessagesViewModel @Inject constructor(
    private val repository: ScheduledMessageRepository,
    private val alarmScheduler: ScheduledMessageAlarmScheduler,
    private val dispatcher: ScheduledMessageDispatcher
) : ViewModel() {

    val scheduledMessages: StateFlow<List<ScheduledMessage>> = repository.observeUpcoming()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun cancel(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            if (repository.cancel(id)) {
                alarmScheduler.cancel(id)
            }
        }
    }

    fun sendNow(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            alarmScheduler.cancel(id)
            dispatcher.sendNowManually(id)
        }
    }

    fun retry(id: Long) = sendNow(id)

    fun update(
        id: Long,
        body: String,
        scheduledForUtcMillis: Long,
        recurrence: RecurrenceRule?,
        attachments: List<com.sotext.domain.model.ScheduledAttachment> = emptyList()
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = repository.getById(id) ?: return@launch
            val updated = existing.copy(
                body = body,
                scheduledForUtcMillis = scheduledForUtcMillis,
                recurrenceRule = recurrence,
                attachments = attachments,
                timezoneId = ZoneId.systemDefault().id
            )
            repository.update(updated)
            alarmScheduler.scheduleExact(updated)
        }
    }
}
