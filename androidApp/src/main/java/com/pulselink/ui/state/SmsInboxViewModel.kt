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
import kotlinx.coroutines.launch

@HiltViewModel
class SmsInboxViewModel @Inject constructor(
    private val smsRepository: SmsRepository
) : ViewModel() {
    private val _threads = MutableStateFlow<List<SmsThreadItem>>(emptyList())
    val threads: StateFlow<List<SmsThreadItem>> = _threads

    fun refresh() {
        viewModelScope.launch {
            _threads.value = smsRepository.listThreads()
        }
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
    }
}
