package com.pulselink.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pulselink.BuildConfig
import com.pulselink.data.ai.AiAssistantRepository
import com.pulselink.data.ai.AiComposeAction
import com.pulselink.data.sms.RemoteSmsRepository
import com.pulselink.data.sms.SmsLineRepository
import com.pulselink.data.sms.SmsMessageItem
import com.pulselink.data.sms.SmsOutboxService
import com.pulselink.data.sms.SmsRepository
import com.pulselink.data.sms.SmsSender
import com.pulselink.data.sms.SmsThreadItem
import com.pulselink.domain.model.Contact
import com.pulselink.domain.model.LineInboxMode
import com.pulselink.domain.model.SmsLine
import com.pulselink.domain.model.ThemePreferences
import com.pulselink.domain.repository.ContactRepository
import com.pulselink.domain.repository.SettingsRepository
import com.pulselink.util.normalizeSmsAddress
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

sealed class SearchResultState {
    object Idle : SearchResultState()
    object Searching : SearchResultState()
    data class Contact(val threadId: Long, val address: String) : SearchResultState()
    data class Messages(val hits: List<SmsMessageItem>) : SearchResultState()
    object Empty : SearchResultState()
}

@HiltViewModel
class SmsInboxViewModel @Inject constructor(
    private val smsRepository: SmsRepository,
    private val remoteSmsRepository: RemoteSmsRepository,
    private val smsLineRepository: SmsLineRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private companion object {
        const val THREAD_LIMIT = Int.MAX_VALUE
    }
    private val localThreads = MutableStateFlow<List<SmsThreadItem>>(emptyList())
    private val localArchived = MutableStateFlow<List<SmsThreadItem>>(emptyList())
    private val remoteThreads = MutableStateFlow<List<SmsThreadItem>>(emptyList())
    private val _threads = MutableStateFlow<List<SmsThreadItem>>(emptyList())
    val threads: StateFlow<List<SmsThreadItem>> = _threads
    private val _archived = MutableStateFlow<List<SmsThreadItem>>(emptyList())
    val archived: StateFlow<List<SmsThreadItem>> = _archived
    private val _searchState = MutableStateFlow<SearchResultState>(SearchResultState.Idle)
    val searchState: StateFlow<SearchResultState> = _searchState
    private val _lines = MutableStateFlow<List<SmsLine>>(emptyList())
    val lines: StateFlow<List<SmsLine>> = _lines
    private val _activeLineId = MutableStateFlow<String?>(null)
    val activeLineId: StateFlow<String?> = _activeLineId
    private val _inboxMode = MutableStateFlow(LineInboxMode.COMBINED)
    val inboxMode: StateFlow<LineInboxMode> = _inboxMode
    private val deviceLineId = MutableStateFlow("")

    fun refresh() {
        viewModelScope.launch {
            val deviceId = deviceLineId.value.ifBlank {
                settingsRepository.ensureDeviceId().also { deviceLineId.value = it }
            }
            localThreads.value = smsRepository.listThreads(limit = THREAD_LIMIT)
                .map { it.copy(lineId = deviceId) }
            localArchived.value = smsRepository.listArchivedThreads(limit = THREAD_LIMIT)
                .map { it.copy(lineId = deviceId) }
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
        viewModelScope.launch {
            deviceLineId.value = settingsRepository.ensureDeviceId()
        }

        settingsRepository.settings
            .onEach { settings ->
                _inboxMode.value = settings.lineInboxMode
                _activeLineId.value = settings.activeLineId ?: settings.deviceId.ifBlank { deviceLineId.value }
                if (settings.deviceId.isNotBlank() && deviceLineId.value != settings.deviceId) {
                    deviceLineId.value = settings.deviceId
                }
            }
            .launchIn(viewModelScope)

        smsLineRepository.observeLines()
            .onEach { lines -> _lines.value = lines.filter { line -> !line.disabled } }
            .launchIn(viewModelScope)

        combine(_lines, _inboxMode, _activeLineId, deviceLineId) { lines, mode, active, deviceId ->
            val lineIds = when (mode) {
                LineInboxMode.COMBINED -> lines.map { it.id }.filter { it.isNotBlank() && it != deviceId }
                LineInboxMode.PER_LINE -> listOfNotNull(active).filter { it.isNotBlank() && it != deviceId }
            }
            lineIds
        }
            .flatMapLatest { lineIds ->
                if (lineIds.isEmpty()) flowOf(emptyList()) else remoteSmsRepository.observeThreads(lineIds)
            }
            .onEach { remoteThreads.value = it }
            .launchIn(viewModelScope)

        combine(
            localThreads,
            localArchived,
            remoteThreads,
            _inboxMode,
            _activeLineId,
            deviceLineId
        ) { local, archived, remote, mode, activeLine, deviceId ->
            val active = activeLine ?: deviceId
            val visibleThreads = when (mode) {
                LineInboxMode.COMBINED -> (local + remote).sortedByDescending { it.timestamp }
                LineInboxMode.PER_LINE -> if (active == deviceId) local else remote.filter { it.lineId == active }
            }
            val visibleArchived = if (mode == LineInboxMode.PER_LINE && active != deviceId) {
                emptyList()
            } else {
                archived
            }
            Pair(visibleThreads, visibleArchived)
        }
            .onEach { (visibleThreads, visibleArchived) ->
                _threads.value = visibleThreads
                _archived.value = visibleArchived
            }
            .launchIn(viewModelScope)

        smsRepository.changes()
            .onEach { refresh() }
            .launchIn(viewModelScope)
    }
}

@HiltViewModel
class SmsThreadViewModel @Inject constructor(
    private val smsRepository: SmsRepository,
    private val contactRepository: ContactRepository,
    private val smsSender: SmsSender,
    private val aiAssistantRepository: AiAssistantRepository,
    private val settingsRepository: SettingsRepository,
    private val remoteSmsRepository: RemoteSmsRepository,
    private val smsOutboxService: SmsOutboxService
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
    private val _summaryState = MutableStateFlow<AiSummaryState>(AiSummaryState.Idle)
    val summaryState: StateFlow<AiSummaryState> = _summaryState
    private val _composeState = MutableStateFlow<AiComposeState>(AiComposeState.Idle)
    val composeState: StateFlow<AiComposeState> = _composeState
    private var activeThreadId: Long? = null
    private var activeAddress: String = ""
    private var activeLineId: String? = null
    private var deviceLineId: String = ""
    private var lastSummaryMessageId: Long? = null
    private var remoteMessagesJob: Job? = null
    private var localMessagesJob: Job? = null

    fun load(threadId: Long, address: String, lineId: String? = null) {
        activeThreadId = threadId.takeIf { it > 0 }
        activeAddress = address
        viewModelScope.launch {
            deviceLineId = settingsRepository.ensureDeviceId()
            activeLineId = lineId ?: deviceLineId
            if (activeAddress.isNotBlank()) {
                _contact.value = contactRepository.getByPhone(activeAddress)
            }
            remoteMessagesJob?.cancel()
            localMessagesJob?.cancel()
            if (!activeLineId.isNullOrBlank() && activeLineId != deviceLineId && activeThreadId != null) {
                remoteMessagesJob = remoteSmsRepository
                    .observeMessages(activeLineId!!, activeThreadId!!)
                    .onEach { _messages.value = it }
                    .launchIn(viewModelScope)
                _isArchived.value = false
            } else {
                refreshMessages()
                localMessagesJob = smsRepository.changes()
                    .onEach { refreshMessages() }
                    .launchIn(viewModelScope)
            }
        }
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
        val latestMessageId = msgs.firstOrNull()?.id
        if (latestMessageId != lastSummaryMessageId) {
            lastSummaryMessageId = null
            if (_summaryState.value !is AiSummaryState.Loading) {
                _summaryState.value = AiSummaryState.Idle
            }
        }
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

    fun sendMessage(address: String, body: String, lineId: String? = null) {
        viewModelScope.launch {
            // Handle multi-recipient (Broadcast) by splitting on common delimiters
            val targetAddress = address.ifBlank { activeAddress }
            if (targetAddress.isBlank()) return@launch
            activeAddress = targetAddress
            val destinations = targetAddress.split(';', ',')
                .map { it.trim() }
                .filter { it.isNotBlank() }
            val resolvedLineId = lineId ?: activeLineId ?: deviceLineId
            val isRemoteLine = resolvedLineId.isNotBlank() && resolvedLineId != deviceLineId

            destinations.forEach { dest ->
                val rawNumber = normalizeSmsAddress(dest)
                if (rawNumber.isNotBlank()) {
                    try {
                        if (isRemoteLine) {
                            smsOutboxService.queueMessage(rawNumber, body, resolvedLineId, activeThreadId)
                        } else {
                            // awaitResult = false to parallelize if multiple
                            smsSender.sendSms(rawNumber, body, awaitResult = false)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            if (!isRemoteLine) {
                refreshMessages()
            }
        }
    }
    fun requestSummary(force: Boolean = false) {
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            val premium = BuildConfig.PREMIUM_FEATURES || settings.premiumUnlocked
            if (!premium || !settings.aiSummariesEnabled) return@launch

            val messages = _messages.value
            if (messages.isEmpty()) return@launch
            val latestMessageId = messages.firstOrNull()?.id
            if (!force && latestMessageId != null && latestMessageId == lastSummaryMessageId) {
                return@launch
            }
            _summaryState.value = AiSummaryState.Loading
            val slices = messages.take(20).reversed().map { msg ->
                val sender = if (msg.outgoing) "Me" else "Them"
                "$sender: ${msg.body}"
            }
            val contactName = _contact.value?.displayName
            val summary = runCatching {
                withTimeout(12_000L) {
                    aiAssistantRepository.summarizeThread(slices, contactName)
                }
            }.getOrElse { error ->
                _summaryState.value = AiSummaryState.Error(error.message ?: "Summary failed.")
                return@launch
            }
            lastSummaryMessageId = latestMessageId
            _summaryState.value = AiSummaryState.Success(summary)
        }
    }

    fun clearSummary() {
        _summaryState.value = AiSummaryState.Idle
    }

    fun requestCompose(
        action: AiComposeAction,
        draft: String?,
        lastMessage: String?
    ) {
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            val premium = BuildConfig.PREMIUM_FEATURES || settings.premiumUnlocked
            if (!premium || !settings.aiComposeEnabled) return@launch

            val resolvedDraft = draft?.trim()
            if (action != AiComposeAction.REPLY && resolvedDraft.isNullOrBlank()) {
                _composeState.value = AiComposeState.Error("Type a draft first.")
                return@launch
            }
            _composeState.value = AiComposeState.Loading
            val suggestion = runCatching {
                withTimeout(12_000L) {
                    aiAssistantRepository.composeSuggestion(
                        action = action,
                        draft = resolvedDraft,
                        lastMessage = lastMessage
                    )
                }
            }.getOrElse { error ->
                _composeState.value = AiComposeState.Error(error.message ?: "AI assist failed.")
                return@launch
            }
            _composeState.value = AiComposeState.Suggestion(suggestion)
        }
    }

    fun clearCompose() {
        _composeState.value = AiComposeState.Idle
    }
}
