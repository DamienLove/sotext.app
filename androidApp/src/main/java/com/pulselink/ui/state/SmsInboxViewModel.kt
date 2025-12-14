package com.pulselink.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pulselink.data.sms.SmsMessageItem
import com.pulselink.data.sms.SmsRepository
import com.pulselink.data.sms.SmsThreadItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@HiltViewModel
class SmsInboxViewModel @Inject constructor(
    private val smsRepository: SmsRepository
) : ViewModel() {
    private val _threads = MutableStateFlow<List<SmsThreadItem>>(emptyList())
    val threads: StateFlow<List<SmsThreadItem>> = _threads
    private val _archived = MutableStateFlow<List<SmsThreadItem>>(emptyList())
    val archived: StateFlow<List<SmsThreadItem>> = _archived

    fun refresh() {
        viewModelScope.launch {
            _threads.value = smsRepository.listThreads()
            _archived.value = smsRepository.listArchivedThreads()
        }
    }

    fun archive(threadId: Long) {
        viewModelScope.launch {
            smsRepository.markThreadRead(threadId)
            smsRepository.archiveThread(threadId)
            refresh()
        }
    }

    fun unarchive(threadId: Long) {
        viewModelScope.launch {
            smsRepository.unarchiveThread(threadId)
            refresh()
        }
    }

    fun delete(threadId: Long) {
        viewModelScope.launch {
            smsRepository.deleteThread(threadId)
            refresh()
        }
    }

    init {
        smsRepository.changes()
            .onEach { refresh() }
            .launchIn(viewModelScope)
    }
}

@HiltViewModel
class SmsThreadViewModel @Inject constructor(
    private val smsRepository: SmsRepository
) : ViewModel() {
    private val _messages = MutableStateFlow<List<SmsMessageItem>>(emptyList())
    val messages: StateFlow<List<SmsMessageItem>> = _messages

    fun load(threadId: Long) {
        viewModelScope.launch {
            _messages.value = smsRepository.messagesForThread(threadId)
        }
        smsRepository.changes()
            .onEach {
                _messages.value = smsRepository.messagesForThread(threadId)
            }
            .launchIn(viewModelScope)
    }
}
