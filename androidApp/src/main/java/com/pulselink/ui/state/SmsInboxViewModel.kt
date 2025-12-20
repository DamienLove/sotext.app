package com.pulselink.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pulselink.data.sms.SmsMessageItem
import com.pulselink.data.sms.SmsRepository
import com.pulselink.data.sms.SmsSender
import com.pulselink.data.sms.SmsThreadItem
import com.pulselink.domain.model.Contact
import com.pulselink.domain.model.ThemePreferences
import com.pulselink.domain.repository.ContactRepository
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
    private val smsRepository: SmsRepository,
    private val contactRepository: ContactRepository,
    private val smsSender: SmsSender
) : ViewModel() {
    private val _messages = MutableStateFlow<List<SmsMessageItem>>(emptyList())
    val messages: StateFlow<List<SmsMessageItem>> = _messages
    private val _contact = MutableStateFlow<Contact?>(null)
    val contact: StateFlow<Contact?> = _contact

    fun load(threadId: Long) {
        viewModelScope.launch {
            val msgs = smsRepository.messagesForThread(threadId)
            _messages.value = msgs
            if (msgs.isNotEmpty()) {
                val address = msgs.first().address
                _contact.value = contactRepository.getByPhone(address)
            }
        }
        smsRepository.changes()
            .onEach {
                val msgs = smsRepository.messagesForThread(threadId)
                _messages.value = msgs
            }
            .launchIn(viewModelScope)
    }

    fun setContactTheme(theme: ThemePreferences?) {
        viewModelScope.launch {
            _contact.value?.let { currentContact ->
                val updated = currentContact.copy(themeOverride = theme)
                contactRepository.upsert(updated)
                _contact.value = updated
            }
        }
    }

    fun sendMessage(address: String, body: String) {
        viewModelScope.launch {
            // Handle multi-recipient (Broadcast) by splitting on semicolon
            val destinations = address.split(";")

            destinations.forEach { dest ->
                // Clean up "Name Ł Number" format if present
                val rawNumber = if (dest.contains(" Ł ")) {
                    dest.split(" Ł ", limit = 2)[1]
                } else {
                    dest
                }

                if (rawNumber.isNotBlank()) {
                    try {
                        // awaitResult = false to parallelize if multiple
                        smsSender.sendSms(rawNumber, body, awaitResult = false)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}
