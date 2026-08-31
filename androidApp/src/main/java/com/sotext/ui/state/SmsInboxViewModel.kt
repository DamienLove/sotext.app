package com.sotext.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sotext.BuildConfig
import com.sotext.data.ai.AiAssistantRepository
import com.sotext.data.ai.AiComposeAction
import com.sotext.data.ai.CatchUpConversationInput
import com.sotext.data.intelligence.CardDecision
import com.sotext.data.intelligence.MessageIntelligenceDecisionEngine
import com.sotext.data.intelligence.MessageIntelligenceRepository
import com.sotext.data.intelligence.MessageIntelligenceResult
import com.sotext.data.intelligence.MessageIntelligenceThresholds
import com.sotext.data.intelligence.MessageIntent
import com.sotext.data.intelligence.ResultSource
import com.sotext.data.intelligence.SecondaryIntent
import com.sotext.domain.model.CatchUpCategory
import com.sotext.domain.model.PulseLinkSettings
import com.sotext.data.sms.RemoteSmsRepository
import com.sotext.data.sms.SmsLineRepository
import com.sotext.data.sms.SmsMessageItem
import com.sotext.data.sms.SmsMessageStatus
import com.sotext.data.sms.SmsOutboxService
import com.sotext.data.sms.SmsRepository
import com.sotext.data.sms.SmsSender
import com.sotext.data.sms.SmsThreadItem
import com.sotext.domain.model.Contact
import com.sotext.domain.model.LineInboxMode
import com.sotext.domain.model.SmsLine
import com.sotext.domain.model.ThemePreferences
import com.sotext.domain.repository.ContactRepository
import com.sotext.domain.repository.SettingsRepository
import com.sotext.util.normalizeSmsAddress
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.math.abs

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
    private val settingsRepository: SettingsRepository,
    private val contactRepository: ContactRepository,
    private val aiAssistantRepository: AiAssistantRepository
) : ViewModel() {
    private companion object {
        const val INITIAL_THREAD_LIMIT = 100
        const val THREAD_PAGE_SIZE = 50
        const val CATCH_UP_MAX_THREADS = 15
        const val CATCH_UP_MESSAGE_LIMIT = 12
        const val CATCH_UP_RECENT_WINDOW_MS = 48L * 60 * 60 * 1000
    }
    private var currentThreadLimit = INITIAL_THREAD_LIMIT
    private val _hasMoreThreads = MutableStateFlow(true)
    val hasMoreThreads: StateFlow<Boolean> = _hasMoreThreads
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
    private val _isDatabaseBusy = MutableStateFlow(false)
    val isDatabaseBusy: StateFlow<Boolean> = _isDatabaseBusy
    private val deviceLineId = MutableStateFlow("")
    private var refreshJob: Job? = null
    private var lastRefreshAt = 0L
    private val _catchMeUpState = MutableStateFlow<CatchMeUpState>(CatchMeUpState.Idle)
    val catchMeUpState: StateFlow<CatchMeUpState> = _catchMeUpState
    private var catchMeUpJob: Job? = null

    fun refresh(force: Boolean = false) {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            val now = System.currentTimeMillis()
            if (!force && now - lastRefreshAt < 1500L) return@launch
            lastRefreshAt = now
            val deviceId = deviceLineId.value.ifBlank {
                settingsRepository.ensureDeviceId().also { deviceLineId.value = it }
            }
            val (threads, archived) = withContext(Dispatchers.IO) {
                smsRepository.listThreadsAndArchived(
                    limit = currentThreadLimit,
                    fromSmsOnly = false,
                    forceRefresh = force
                )
            }
            if (!force &&
                threads.isEmpty() &&
                archived.isEmpty() &&
                (localThreads.value.isNotEmpty() || localArchived.value.isNotEmpty())
            ) {
                return@launch
            }
            localThreads.value = threads.map { it.copy(lineId = deviceId) }
            localArchived.value = archived.map { it.copy(lineId = deviceId) }
            _hasMoreThreads.value = threads.size >= currentThreadLimit
        }
    }

    fun loadMoreThreads() {
        if (refreshJob?.isActive == true) return
        currentThreadLimit += THREAD_PAGE_SIZE
        refresh(force = true)
    }

    fun importAllMessages() {
        viewModelScope.launch {
            runDatabaseAction {
                val deviceId = deviceLineId.value.ifBlank {
                    settingsRepository.ensureDeviceId().also { deviceLineId.value = it }
                }
                val (threads, archived) = withContext(Dispatchers.IO) {
                    smsRepository.listThreadsAndArchived(
                        limit = currentThreadLimit,
                        fromSmsOnly = true,
                        forceRefresh = true
                    )
                }
                localThreads.value = threads.map { it.copy(lineId = deviceId) }
                localArchived.value = archived.map { it.copy(lineId = deviceId) }
            }
        }
    }

    fun pin(threadId: Long) {
        viewModelScope.launch {
            runDatabaseAction {
                withContext(Dispatchers.IO) {
                    smsRepository.pinThread(threadId)
                }
                refresh(force = true)
            }
        }
    }

    fun unpin(threadId: Long) {
        viewModelScope.launch {
            runDatabaseAction {
                withContext(Dispatchers.IO) {
                    smsRepository.unpinThread(threadId)
                }
                refresh(force = true)
            }
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
            runDatabaseAction {
                withContext(Dispatchers.IO) {
                    val blockReceipts = settingsRepository.settings.first().blockRcsReadReceipts
                    smsRepository.markThreadRead(threadId, blockReadReceipts = blockReceipts)
                    smsRepository.archiveThread(threadId)
                }
                refresh(force = true)
            }
        }
    }

    fun unarchive(threadId: Long) {
        viewModelScope.launch {
            runDatabaseAction {
                withContext(Dispatchers.IO) {
                    smsRepository.unarchiveThread(threadId)
                }
                refresh(force = true)
            }
        }
    }

    fun delete(threadId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            runDatabaseAction {
                smsRepository.deleteThread(threadId)
                refresh()
            }
        }
    }

    /**
     * Builds an AI briefing across unread/recently-active conversations, grouped into
     * "needs your response" / "important updates" / "no action needed". Grounded only in
     * each thread's own recent messages - see [CatchUpConversationInput].
     */
    fun requestCatchMeUp(force: Boolean = false) {
        if (catchMeUpJob?.isActive == true && !force) return
        catchMeUpJob?.cancel()
        catchMeUpJob = viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            val premium = BuildConfig.PREMIUM_FEATURES || settings.premiumUnlocked
            if (!premium || !settings.catchMeUpEnabled) {
                _catchMeUpState.value = CatchMeUpState.Error("Catch Me Up isn't enabled.")
                return@launch
            }

            _catchMeUpState.value = CatchMeUpState.Loading

            val now = System.currentTimeMillis()
            // Fetch a fresh snapshot directly rather than relying on the reactive `threads` state,
            // so this works correctly regardless of whether `refresh()` has populated it yet.
            val freshThreads = withContext(Dispatchers.IO) {
                runCatching {
                    smsRepository.listThreadsAndArchived(
                        limit = CATCH_UP_MAX_THREADS * 4,
                        fromSmsOnly = false,
                        forceRefresh = true
                    ).first
                }.getOrDefault(emptyList())
            }
            val candidates = freshThreads
                .asSequence()
                .filter { !it.isPrivate }
                .filter { it.unread || (now - it.timestamp) <= CATCH_UP_RECENT_WINDOW_MS }
                .sortedByDescending { it.timestamp }
                .take(CATCH_UP_MAX_THREADS)
                .toList()

            if (candidates.isEmpty()) {
                _catchMeUpState.value = CatchMeUpState.Success(emptyList(), emptyList(), emptyList(), now)
                return@launch
            }

            data class Conversation(val thread: SmsThreadItem, val contactName: String?, val lines: List<String>)

            val conversations = withContext(Dispatchers.IO) {
                candidates.mapNotNull { thread ->
                    val messages = runCatching {
                        smsRepository.messagesForThread(thread.threadId, CATCH_UP_MESSAGE_LIMIT)
                    }.getOrDefault(emptyList())
                    if (messages.isEmpty()) return@mapNotNull null
                    val contactName = runCatching {
                        contactRepository.getByPhone(thread.address)?.displayName
                    }.getOrNull()?.takeIf { it.isNotBlank() }
                    val lines = messages.reversed().map { msg ->
                        val sender = if (msg.outgoing) "Me" else (contactName ?: "Them")
                        val body = msg.body.takeIf { it.isNotBlank() && it != "[MMS]" } ?: "[sent an attachment]"
                        "$sender: $body"
                    }
                    Conversation(thread, contactName, lines)
                }
            }

            if (conversations.isEmpty()) {
                _catchMeUpState.value = CatchMeUpState.Success(emptyList(), emptyList(), emptyList(), now)
                return@launch
            }

            val requestInputs = conversations.map {
                CatchUpConversationInput(threadId = it.thread.threadId, contactName = it.contactName, messages = it.lines)
            }

            val results = runCatching {
                withTimeout(20_000L) { aiAssistantRepository.catchMeUp(requestInputs) }
            }.getOrElse { error ->
                _catchMeUpState.value = CatchMeUpState.Error(error.message ?: "Couldn't build your briefing.")
                return@launch
            }

            val resultsByThread = results.associateBy { it.threadId }
            val needsResponse = mutableListOf<CatchUpCard>()
            val importantUpdates = mutableListOf<CatchUpCard>()
            val noAction = mutableListOf<CatchUpCard>()
            conversations.forEach { conversation ->
                val result = resultsByThread[conversation.thread.threadId] ?: return@forEach
                val card = CatchUpCard(thread = conversation.thread, contactName = conversation.contactName, result = result)
                when (result.category) {
                    CatchUpCategory.NEEDS_RESPONSE -> needsResponse += card
                    CatchUpCategory.IMPORTANT_UPDATE -> importantUpdates += card
                    CatchUpCategory.NO_ACTION -> noAction += card
                }
            }

            _catchMeUpState.value = CatchMeUpState.Success(
                needsResponse = needsResponse.sortedByDescending { it.thread.timestamp },
                importantUpdates = importantUpdates.sortedByDescending { it.thread.timestamp },
                noActionNeeded = noAction.sortedByDescending { it.thread.timestamp },
                generatedAt = now
            )
        }
    }

    /** Removes a card from the current briefing (e.g. after "Mark as handled") without touching the thread itself. */
    fun dismissCatchUpCard(threadId: Long) {
        val state = _catchMeUpState.value as? CatchMeUpState.Success ?: return
        _catchMeUpState.value = state.copy(
            needsResponse = state.needsResponse.filterNot { it.thread.threadId == threadId },
            importantUpdates = state.importantUpdates.filterNot { it.thread.threadId == threadId },
            noActionNeeded = state.noActionNeeded.filterNot { it.thread.threadId == threadId }
        )
    }

    fun clearCatchMeUp() {
        catchMeUpJob?.cancel()
        _catchMeUpState.value = CatchMeUpState.Idle
    }

    private suspend fun runDatabaseAction(block: suspend () -> Unit) {
        _isDatabaseBusy.value = true
        try {
            block()
        } finally {
            _isDatabaseBusy.value = false
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
) { values ->
            val local = values[0] as List<SmsThreadItem>
            val archived = values[1] as List<SmsThreadItem>
            val remote = values[2] as List<SmsThreadItem>
            val mode = values[3] as LineInboxMode
            val activeLine = values[4] as String?
            val deviceId = values[5] as String
            val active = activeLine ?: deviceId
            val visibleThreads = when (mode) {
                LineInboxMode.COMBINED -> (local + remote).sortedByDescending { it.timestamp }
                LineInboxMode.PER_LINE -> {
                    val list = if (active == deviceId) local else remote.filter { it.lineId == active }
                    list.sortedByDescending { it.timestamp }
                }
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
            .debounce(100)
            .onEach { refresh(force = true) }
            .launchIn(viewModelScope)

        contactRepository.observeContacts()
            .debounce(300)
            .onEach { refresh(force = true) }
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
    private val smsOutboxService: SmsOutboxService,
    private val messageIntelligenceRepository: MessageIntelligenceRepository
) : ViewModel() {
    private companion object {
        const val PENDING_MATCH_WINDOW_MS = 20_000L
        const val INITIAL_MESSAGE_LIMIT = 100
        const val MESSAGE_PAGE_SIZE = 50
    }
    private var currentMessageLimit = INITIAL_MESSAGE_LIMIT
    private val _messages = MutableStateFlow<List<SmsMessageItem>>(emptyList())
    val messages: StateFlow<List<SmsMessageItem>> = _messages
    private val _contact = MutableStateFlow<Contact?>(null)
    val contact: StateFlow<Contact?> = _contact
    private val _isArchived = MutableStateFlow(false)
    val isArchived: StateFlow<Boolean> = _isArchived
    private val _isPinned = MutableStateFlow(false)
    val isPinned: StateFlow<Boolean> = _isPinned
    private val _summaryState = MutableStateFlow<AiSummaryState>(AiSummaryState.Idle)
    val summaryState: StateFlow<AiSummaryState> = _summaryState
    private val _composeState = MutableStateFlow<AiComposeState>(AiComposeState.Idle)
    val composeState: StateFlow<AiComposeState> = _composeState
    private val _messageIntelligence = MutableStateFlow<Map<Long, CardDecision>>(emptyMap())
    val messageIntelligence: StateFlow<Map<Long, CardDecision>> = _messageIntelligence
    private val requestedIntelligenceFor = mutableSetOf<Long>()
    private val _isDatabaseBusy = MutableStateFlow(false)
    val isDatabaseBusy: StateFlow<Boolean> = _isDatabaseBusy
    private val _hasMoreMessages = MutableStateFlow(false)
    val hasMoreMessages: StateFlow<Boolean> = _hasMoreMessages
    private var activeThreadId: Long? = null
    private var activeAddress: String = ""
    private var activeLineId: String? = null
    private var deviceLineId: String = ""
    private var lastSummaryMessageId: Long? = null
    private var remoteMessagesJob: Job? = null
    private var localMessagesJob: Job? = null
    private val pendingMessages = mutableListOf<SmsMessageItem>()
    private val pendingLock = Any()
    private var loadedMessages: List<SmsMessageItem> = emptyList()

    fun load(threadId: Long, address: String, lineId: String? = null) {
        activeThreadId = threadId.takeIf { it > 0 }
        activeAddress = address
        currentMessageLimit = 100
        resetMessages()
        viewModelScope.launch {
            deviceLineId = settingsRepository.ensureDeviceId()
            activeLineId = lineId ?: deviceLineId
            if (activeAddress.isNotBlank()) {
                _contact.value = withContext(Dispatchers.IO) {
                    contactRepository.getByPhone(activeAddress)
                }
            }
            remoteMessagesJob?.cancel()
            localMessagesJob?.cancel()
            if (!activeLineId.isNullOrBlank() && activeLineId != deviceLineId && activeThreadId != null) {
                remoteMessagesJob = remoteSmsRepository
                    .observeMessages(activeLineId!!, activeThreadId!!)
                    .onEach { updateMessages(it) }
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
        val result = withContext(Dispatchers.IO) {
            if (activeThreadId == null && activeAddress.isNotBlank()) {
                activeThreadId = smsRepository.resolveThreadIdForAddress(activeAddress)
            }
            val msgs = when {
                activeThreadId != null -> smsRepository.messagesForThread(activeThreadId!!, limit = currentMessageLimit)
                activeAddress.isNotBlank() -> smsRepository.messagesForAddress(activeAddress, limit = currentMessageLimit)
                else -> emptyList()
            }
            val contact = msgs.firstOrNull()?.address?.let { contactRepository.getByPhone(it) }
            val archived = activeThreadId?.let { smsRepository.isThreadArchived(it) } ?: false
            val pinned = activeThreadId?.let { smsRepository.isThreadPinned(it) } ?: false
            Triple(msgs, contact, Pair(archived, pinned))
        }
        val msgs = result.first
        val contact = result.second
        val (archived, pinned) = result.third
        activeThreadId?.let { threadId ->
            val blockReceipts = settingsRepository.settings.first().blockRcsReadReceipts
            withContext(Dispatchers.IO) { smsRepository.markThreadRead(threadId, blockReadReceipts = blockReceipts) }
        }
        updateMessages(msgs)
        _contact.value = contact
        _isArchived.value = archived
        _isPinned.value = pinned
        _hasMoreMessages.value = msgs.size >= currentMessageLimit
    }

    fun toggleArchive() {
        viewModelScope.launch {
            runDatabaseAction {
                if (activeThreadId == null && activeAddress.isNotBlank()) {
                    activeThreadId = smsRepository.resolveThreadIdForAddress(activeAddress)
                }
                val threadId = activeThreadId ?: return@runDatabaseAction
                val updatedArchived = withContext(Dispatchers.IO) {
                    if (_isArchived.value) {
                        smsRepository.unarchiveThread(threadId)
                    } else {
                        smsRepository.archiveThread(threadId)
                    }
                    smsRepository.isThreadArchived(threadId)
                }
                _isArchived.value = updatedArchived
            }
        }
    }

    fun togglePin() {
        viewModelScope.launch {
            runDatabaseAction {
                if (activeThreadId == null && activeAddress.isNotBlank()) {
                    activeThreadId = smsRepository.resolveThreadIdForAddress(activeAddress)
                }
                val threadId = activeThreadId ?: return@runDatabaseAction
                val updatedPinned = withContext(Dispatchers.IO) {
                    if (_isPinned.value) {
                        smsRepository.unpinThread(threadId)
                    } else {
                        smsRepository.pinThread(threadId)
                    }
                    smsRepository.isThreadPinned(threadId)
                }
                _isPinned.value = updatedPinned
            }
        }
    }

    fun setContactTheme(theme: ThemePreferences?) {
        viewModelScope.launch(Dispatchers.IO) {
            runDatabaseAction {
                _contact.value?.let { currentContact ->
                    val updated = currentContact.copy(themeOverride = theme)
                    contactRepository.upsert(updated)
                    withContext(Dispatchers.Main) {
                        _contact.value = updated
                    }
                }
            }
        }
    }

    private suspend fun runDatabaseAction(block: suspend () -> Unit) {
        _isDatabaseBusy.value = true
        try {
            block()
        } finally {
            _isDatabaseBusy.value = false
        }
    }

    fun sendMessage(address: String, body: String, lineId: String? = null) {
        viewModelScope.launch {
            // Handle multi-recipient (Broadcast) by splitting on common delimiters
            val targetAddress = address.ifBlank { activeAddress }
            if (targetAddress.isBlank()) return@launch
            activeAddress = targetAddress
            val sendTimestamp = System.currentTimeMillis()
            val pendingId = -sendTimestamp
            addPendingMessage(
                SmsMessageItem(
                    id = pendingId,
                    threadId = activeThreadId ?: pendingId,
                    address = targetAddress,
                    body = body,
                    timestamp = sendTimestamp,
                    outgoing = true,
                    status = SmsMessageStatus.SENDING
                )
            )
            val destinations = targetAddress.split(';', ',')
                .map { it.trim() }
                .filter { it.isNotBlank() }
            val resolvedLineId = lineId ?: activeLineId ?: deviceLineId
            val isRemoteLine = resolvedLineId.isNotBlank() && resolvedLineId != deviceLineId
            var anySuccess = false

            withContext(Dispatchers.IO) {
                destinations.forEach { dest ->
                    val rawNumber = normalizeSmsAddress(dest)
                    if (rawNumber.isNotBlank()) {
                        try {
                            if (isRemoteLine) {
                                smsOutboxService.queueMessage(rawNumber, body, resolvedLineId, activeThreadId)
                                anySuccess = true
                            } else {
                                // awaitResult = false to parallelize if multiple
                                val sent = smsSender.sendSms(rawNumber, body, awaitResult = false)
                                anySuccess = anySuccess || sent
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
            if (!isRemoteLine) {
                refreshMessages()
            }
            if (!anySuccess) {
                markPendingFailed(pendingId)
            }
        }
    }

    private fun updateMessages(messages: List<SmsMessageItem>) {
        withPendingLock {
            loadedMessages = messages
            _messages.value = mergeMessages(messages)
        }
        val latestMessageId = messages.firstOrNull()?.id
        if (latestMessageId != lastSummaryMessageId) {
            lastSummaryMessageId = null
            if (_summaryState.value !is AiSummaryState.Loading) {
                _summaryState.value = AiSummaryState.Idle
            }
        }
    }

    private fun resetMessages() {
        withPendingLock {
            pendingMessages.clear()
            loadedMessages = emptyList()
            _messages.value = emptyList()
        }
    }

    private fun addPendingMessage(message: SmsMessageItem) {
        withPendingLock {
            pendingMessages.add(message)
            _messages.value = mergeMessages(loadedMessages)
        }
    }

    private fun markPendingFailed(pendingId: Long) {
        withPendingLock {
            pendingMessages.replaceAll { msg ->
                if (msg.id == pendingId) msg.copy(status = SmsMessageStatus.FAILED) else msg
            }
            _messages.value = mergeMessages(loadedMessages)
        }
    }

    private fun mergeMessages(messages: List<SmsMessageItem>): List<SmsMessageItem> {
        if (pendingMessages.isEmpty()) return messages
        val pendingRemaining = pendingMessages.filterNot { pending ->
            messages.any { loaded -> isSameMessage(loaded, pending) }
        }
        pendingMessages.clear()
        pendingMessages.addAll(pendingRemaining)
        return (messages + pendingRemaining).sortedByDescending { it.timestamp }
    }

    private fun isSameMessage(loaded: SmsMessageItem, pending: SmsMessageItem): Boolean {
        if (loaded.outgoing != pending.outgoing) return false
        if (loaded.body != pending.body) return false
        val timeDelta = abs(loaded.timestamp - pending.timestamp)
        if (timeDelta > PENDING_MATCH_WINDOW_MS) return false
        val loadedAddress = normalizeSmsAddress(loaded.address)
        val pendingAddress = normalizeSmsAddress(pending.address)
        if (loadedAddress.isNotBlank() && pendingAddress.isNotBlank()) {
            return loadedAddress == pendingAddress
        }
        return true
    }

    private fun <T> withPendingLock(block: () -> T): T = synchronized(pendingLock) {
        block()
    }

    fun requestSummary(force: Boolean = false) {
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            val premium = BuildConfig.PREMIUM_FEATURES || settings.premiumUnlocked
            if (!premium || !settings.aiSummariesEnabled) return@launch

            val messages = loadedMessages
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

    fun loadMoreMessages() {
        currentMessageLimit += MESSAGE_PAGE_SIZE
        viewModelScope.launch {
            refreshMessages()
        }
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

    /**
     * Runs the Message Intelligence pipeline for one message: an immediate, free, on-device
     * pass (same guarantee as [com.sotext.data.context.MessageContextParser] - nothing leaves
     * the device), then - only if Premium and the cloud deep-pass is opted in, and only if this
     * message hasn't already had its one cloud pass attempted (see
     * [MessageIntelligenceRepository.cloudPassAttempted]) - an async safety classification
     * (attempted regardless of on-device confidence, since safety can override an otherwise-fine
     * message) and, only when the on-device result wasn't already confident, an async intent
     * deep-pass. Each step publishes through [messageIntelligence] as soon as it resolves; a
     * later cloud result can only upgrade what's already showing (see
     * [MessageIntelligenceRepository]), never replace it with something weaker. Safe to call
     * once per message per screen visit - [requestedIntelligenceFor] makes repeat calls (e.g.
     * from LazyColumn recomposition) no-ops within one screen visit, and the repository-level
     * flag prevents a re-issue across separate visits (e.g. leaving and reopening the thread).
     */
    fun requestMessageIntelligence(messageId: Long, body: String, timestampMillis: Long, senderAddress: String?) {
        if (!requestedIntelligenceFor.add(messageId)) return
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            if (!settings.messageIntelligenceEnabled) return@launch

            val onDevice = messageIntelligenceRepository.analyzeOnDevice(messageId, body, timestampMillis, senderAddress)
            publishDecision(messageId, onDevice, settings)

            val premium = BuildConfig.PREMIUM_FEATURES || settings.premiumUnlocked
            if (!premium || !settings.messageIntelligenceCloudEnabled) return@launch
            // The repository's cloud-attempted flag is process-lifetime, not just this
            // ViewModel's - so reopening the same thread doesn't re-issue paid AI calls for a
            // message that already got its one cloud pass (see MessageIntelligenceRepository).
            if (messageIntelligenceRepository.cloudPassAttempted(messageId)) return@launch
            messageIntelligenceRepository.markCloudPassAttempted(messageId)

            val safety = runCatching { withTimeout(6_000L) { aiAssistantRepository.classifySafety(body) } }.getOrNull()
            if (safety != null) {
                publishDecision(messageId, messageIntelligenceRepository.mergeCloudSafety(messageId, safety), settings)
            }

            if (onDevice.confidence < MessageIntelligenceThresholds.DEFAULT.highConfidence) {
                val cloud = runCatching { withTimeout(6_000L) { aiAssistantRepository.classifyMessageIntent(body) } }.getOrNull()
                if (cloud != null) {
                    val cloudResult = MessageIntelligenceResult(
                        messageId = messageId,
                        intent = cloud.intent,
                        confidence = cloud.confidence,
                        actionability = cloud.actionability,
                        entities = cloud.entities,
                        suggestedActions = messageIntelligenceRepository.actionsFor(cloud.intent),
                        secondaryIntents = listOfNotNull(
                            cloud.secondaryIntent?.let {
                                SecondaryIntent(it.intent, it.confidence, it.actionability, it.entities)
                            }
                        ),
                        source = ResultSource.CLOUD
                    )
                    publishDecision(messageId, messageIntelligenceRepository.mergeCloudIntent(messageId, cloudResult), settings)
                }
            }
        }
    }

    private fun publishDecision(messageId: Long, result: MessageIntelligenceResult, settings: PulseLinkSettings) {
        val suppressed = settings.suppressedIntelligenceCardTypes
            .mapNotNull { raw -> runCatching { MessageIntent.valueOf(raw) }.getOrNull() }
            .toSet()
        val decision = MessageIntelligenceDecisionEngine.decide(result, suppressedTypes = suppressed)
        _messageIntelligence.value = _messageIntelligence.value + (messageId to decision)
    }
}

