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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class SearchResultState {
    object Idle : SearchResultState()
    object Searching : SearchResultState()
    data class Contact(val threadId: Long, val address: String) : SearchResultState()
    data class Messages(val hits: List<SmsMessageItem>) : SearchResultState()
    object Empty : SearchResultState()
}

@HiltViewModel
class SmsInboxViewModel @Inject constructor(
    private val smsRepository: SmsRepository
) : ViewModel() {
    private companion object {
        const val THREAD_LIMIT = Int.MAX_VALUE
    }
    private val _threads = MutableStateFlow<List<SmsThreadItem>>(emptyList())
    val threads: StateFlow<List<SmsThreadItem>> = _threads
    private val _archived = MutableStateFlow<List<SmsThreadItem>>(emptyList())
    val archived: StateFlow<List<SmsThreadItem>> = _archived
    private val _searchState = MutableStateFlow<SearchResultState>(SearchResultState.Idle)
    val searchState: StateFlow<SearchResultState> = _searchState

    fun refresh() {
        viewModelScope.launch {
            _threads.value = smsRepository.listThreads(limit = THREAD_LIMIT)
            _archived.value = smsRepository.listArchivedThreads(limit = THREAD_LIMIT)
        }
    }

    fun search(query: String) {
        if (query.isBlank()) {
            _searchState.value = SearchResultState.Idle
            return
        }
        _searchState.value = SearchResultState.Searching
        viewModelScope.launch(Dispatchers.IO) {
            val direct = _threads.value.firstOrNull {
                it.address.contains(query, ignoreCase = true) ||
                    it.snippet.contains(query, ignoreCase = true)
            }
            if (direct != null) {
                withContext(Dispatchers.Main) {
                    _searchState.value = SearchResultState.Contact(direct.threadId, direct.address)
                }
                return@launch
            }

            val hits = smsRepository.searchMessages(query)
            withContext(Dispatchers.Main) {
                _searchState.value = if (hits.isEmpty()) SearchResultState.Empty else SearchResultState.Messages(hits)
            }
        }
    }

    fun clearSearch() {
        _searchState.value = SearchResultState.Idle
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
    private companion object {
        const val MESSAGE_LIMIT = Int.MAX_VALUE
    }
    private val _messages = MutableStateFlow<List<SmsMessageItem>>(emptyList())
    val messages: StateFlow<List<SmsMessageItem>> = _messages
    private val _contact = MutableStateFlow<Contact?>(null)
    val contact: StateFlow<Contact?> = _contact
    private val _isArchived = MutableStateFlow(false)
    val isArchived: StateFlow<Boolean> = _isArchived
    private var activeThreadId: Long? = null
    private var activeAddress: String = ""

    fun load(threadId: Long, address: String) {
        activeThreadId = threadId.takeIf { it > 0 }
        activeAddress = address
        viewModelScope.launch {
            refreshMessages()
        }
        smsRepository.changes()
            .onEach { refreshMessages() }
            .launchIn(viewModelScope)
    }

    private suspend fun refreshMessages() {
        if (activeThreadId == null && activeAddress.isNotBlank()) {
            activeThreadId = smsRepository.resolveThreadIdForAddress(activeAddress)
        }
        val msgs = when {
            activeThreadId != null -> smsRepository.messagesForThread(activeThreadId!!, limit = MESSAGE_LIMIT)
            activeAddress.isNotBlank() -> smsRepository.messagesForAddress(activeAddress, limit = MESSAGE_LIMIT)
            else -> emptyList()
        }
        _messages.value = msgs
        if (msgs.isNotEmpty()) {
            val address = msgs.first().address
            _contact.value = contactRepository.getByPhone(address)
        }
        activeThreadId?.let { threadId ->
            _isArchived.value = smsRepository.isThreadArchived(threadId)
        }
    }

    fun toggleArchive() {
        viewModelScope.launch {
            if (activeThreadId == null && activeAddress.isNotBlank()) {
                activeThreadId = smsRepository.resolveThreadIdForAddress(activeAddress)
            }
            val threadId = activeThreadId ?: return@launch
            if (_isArchived.value) {
                smsRepository.unarchiveThread(threadId)
            } else {
                smsRepository.archiveThread(threadId)
            }
            _isArchived.value = !_isArchived.value
        }
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
            val targetAddress = address.ifBlank { activeAddress }
            if (targetAddress.isBlank()) return@launch
            activeAddress = targetAddress
            val destinations = targetAddress.split(";")

            destinations.forEach { dest ->
                // Clean up "Name Ł Number" or "Name ú Number" formats if present
                val rawNumber = when {
                    dest.contains(" Ł ") -> dest.split(" Ł ", limit = 2)[1]
                    dest.contains(" ú ") -> dest.split(" ú ", limit = 2)[1]
                    else -> dest
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
            refreshMessages()
        }
    }
}
