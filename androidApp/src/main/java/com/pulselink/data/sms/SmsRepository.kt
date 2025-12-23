package com.pulselink.data.sms

import android.content.Context
import android.provider.Telephony
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import com.pulselink.data.db.ArchivedThreadDao
import com.pulselink.data.db.ContactDao
import com.pulselink.domain.model.ArchivedThread
import com.pulselink.domain.model.EscalationTier
import com.pulselink.domain.model.MessageUrgency
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.os.Build
import kotlin.jvm.Volatile
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import android.provider.ContactsContract
import android.net.Uri
import android.app.role.RoleManager

@Singleton
class SmsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val archivedThreadDao: ArchivedThreadDao,
    private val contactDao: ContactDao
) {

    @Volatile private var observersRegistered = false
    private val observerFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    init {
        ensureObserversRegistered()
    }

    fun changes(): SharedFlow<Unit> = observerFlow.asSharedFlow()

    suspend fun listThreads(
        limit: Int = 50,
        includeArchived: Boolean = false,
        onlyArchived: Boolean = false
    ): List<SmsThreadItem> {
        if (!hasReadPerms()) return emptyList()
        ensureObserversRegistered()
        val archivedIds = runCatching { archivedThreadDao.getAllIds() }.getOrDefault(emptyList())
        val projection = arrayOf(
            Telephony.Threads._ID,
            Telephony.Threads.DATE,
            Telephony.Threads.MESSAGE_COUNT,
            Telephony.Threads.READ
        )
        val cursor = runCatching {
            context.contentResolver.query(
                Telephony.Threads.CONTENT_URI,
                projection,
                null,
                null,
                "${Telephony.Threads.DATE} DESC"
            )
        }.getOrNull()

        cursor ?: return emptyList()
        cursor.use { c ->
            val idIdx = c.getColumnIndexOrThrow(Telephony.Threads._ID)
            val readIdx = c.getColumnIndexOrThrow(Telephony.Threads.READ)
            val items = mutableListOf<SmsThreadItem>()
            var count = 0
            while (c.moveToNext() && count < limit) {
                val threadId = c.getLong(idIdx)

                // Always fetch the latest valid message for this thread to ensure we get
                // the correct address (Telephony.Threads does not provide it directly)
                // and to filter out internal PulseLink payloads.
                val lastMsg = getLastValidMessageForThread(threadId) ?: continue

                val snippet = lastMsg.body
                val ts = lastMsg.timestamp
                val unread = c.getInt(readIdx) == 0
                val address = resolveAddress(lastMsg.address)
                val isArchived = archivedIds.contains(threadId)

                val parts = address.split(" · ")
                val phone = if (parts.size > 1) parts[1] else parts[0]
                val normalized = normalizePhone(phone)
                val contact = contactDao.getByPhone(phone)
                    ?: contactDao.getByPhone(normalized)

                if (onlyArchived && !isArchived) {
                    // skip
                } else if (!includeArchived && isArchived) {
                    // skip
                } else {
                    val trustedUrgency = contact?.let {
                        when {
                            OtpHelper.isUrgentBody(snippet) -> MessageUrgency.URGENT
                            it.escalationTier == EscalationTier.EMERGENCY -> MessageUrgency.EMERGENCY
                            else -> MessageUrgency.STANDARD
                        }
                    }
                    items += SmsThreadItem(
                        threadId = threadId,
                        address = address,
                        snippet = snippet,
                        timestamp = ts,
                        unread = unread,
                        isPrivate = contact?.isPrivate == true,
                        isFavorite = contact?.isFavorite == true,
                        isTrusted = contact != null,
                        trustedUrgency = trustedUrgency,
                        isOtp = OtpHelper.isOtpMessage(phone, snippet)
                    )
                }
                count++
            }
            if (items.isNotEmpty()) {
                return items
            }
        }
        return listThreadsFromSms(limit, archivedIds, includeArchived, onlyArchived)
    }

    suspend fun listArchivedThreads(limit: Int = 50): List<SmsThreadItem> =
        listThreads(limit = limit, includeArchived = true, onlyArchived = true)

    fun searchMessages(query: String, limit: Int = 40): List<SmsMessageItem> {
        if (!hasReadPerms() || query.isBlank()) return emptyList()
        val pattern = "%${query.trim()}%"
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE
        )
        val cursor = runCatching {
            context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                projection,
                "${Telephony.Sms.BODY} LIKE ?",
                arrayOf(pattern),
                "${Telephony.Sms.DATE} DESC"
            )
        }.getOrNull() ?: return emptyList()

        cursor.use { c ->
            val idIdx = c.getColumnIndexOrThrow(Telephony.Sms._ID)
            val threadIdx = c.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
            val addrIdx = c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIdx = c.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIdx = c.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val typeIdx = c.getColumnIndexOrThrow(Telephony.Sms.TYPE)
            val hits = mutableListOf<SmsMessageItem>()
            var count = 0
            while (c.moveToNext() && count < limit) {
                val body = c.getString(bodyIdx) ?: ""
                if (SmsCodec.isPulseLinkPayload(body)) {
                    count++
                    continue
                }
                val outgoing = c.getInt(typeIdx) == Telephony.Sms.MESSAGE_TYPE_SENT ||
                    c.getInt(typeIdx) == Telephony.Sms.MESSAGE_TYPE_OUTBOX
                hits += SmsMessageItem(
                    id = c.getLong(idIdx),
                    threadId = c.getLong(threadIdx),
                    address = resolveAddress(c.getString(addrIdx)),
                    body = body,
                    timestamp = c.getLong(dateIdx),
                    outgoing = outgoing
                )
                count++
            }
            return hits
        }
    }

    fun messagesForThread(threadId: Long, limit: Int = 200): List<SmsMessageItem> {
        if (!hasReadPerms()) return emptyList()
        ensureObserversRegistered()
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE
        )
        val cursor = runCatching {
            context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                projection,
                "${Telephony.Sms.THREAD_ID}=?",
                arrayOf(threadId.toString()),
                "${Telephony.Sms.DATE} DESC"
            )
        }.getOrNull()
        cursor ?: return emptyList()
        cursor.use { c ->
            val idIdx = c.getColumnIndexOrThrow(Telephony.Sms._ID)
            val threadIdx = c.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
            val addrIdx = c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIdx = c.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIdx = c.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val typeIdx = c.getColumnIndexOrThrow(Telephony.Sms.TYPE)
            val items = mutableListOf<SmsMessageItem>()
            var count = 0
            while (c.moveToNext() && count < limit) {
                val id = c.getLong(idIdx)
                val addr = resolveAddress(c.getString(addrIdx))
                val body = c.getString(bodyIdx) ?: ""
                if (SmsCodec.isPulseLinkPayload(body)) {
                    count++
                    continue
                }
                val ts = c.getLong(dateIdx)
                val type = c.getInt(typeIdx)
                val outgoing = type == Telephony.Sms.MESSAGE_TYPE_SENT || type == Telephony.Sms.MESSAGE_TYPE_OUTBOX
                items += SmsMessageItem(
                    id = id,
                    threadId = c.getLong(threadIdx),
                    address = addr,
                    body = body,
                    timestamp = ts,
                    outgoing = outgoing
                )
                count++
            }
            return items.sortedBy { it.timestamp }
        }
    }

    fun messagesForAddress(address: String, limit: Int = 200): List<SmsMessageItem> {
        if (!hasReadPerms()) return emptyList()
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE
        )
        val candidates = addressCandidates(address)
        if (candidates.isEmpty()) return emptyList()
        val items = mutableListOf<SmsMessageItem>()
        candidates.forEach { candidate ->
            val cursor = runCatching {
                context.contentResolver.query(
                    Telephony.Sms.CONTENT_URI,
                    projection,
                    "${Telephony.Sms.ADDRESS}=?",
                    arrayOf(candidate),
                    "${Telephony.Sms.DATE} DESC"
                )
            }.getOrNull() ?: return@forEach
            cursor.use { c ->
                val idIdx = c.getColumnIndexOrThrow(Telephony.Sms._ID)
                val threadIdx = c.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
                val addrIdx = c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val bodyIdx = c.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val dateIdx = c.getColumnIndexOrThrow(Telephony.Sms.DATE)
                val typeIdx = c.getColumnIndexOrThrow(Telephony.Sms.TYPE)
                var count = 0
                while (c.moveToNext() && count < limit) {
                    val body = c.getString(bodyIdx) ?: ""
                    if (SmsCodec.isPulseLinkPayload(body)) {
                        count++
                        continue
                    }
                    val outgoing = c.getInt(typeIdx) == Telephony.Sms.MESSAGE_TYPE_SENT ||
                        c.getInt(typeIdx) == Telephony.Sms.MESSAGE_TYPE_OUTBOX
                    items += SmsMessageItem(
                        id = c.getLong(idIdx),
                        threadId = c.getLong(threadIdx),
                        address = resolveAddress(c.getString(addrIdx)),
                        body = body,
                        timestamp = c.getLong(dateIdx),
                        outgoing = outgoing
                    )
                    count++
                }
            }
        }
        return items.distinctBy { it.id }.sortedBy { it.timestamp }.takeLast(limit)
    }

    fun resolveThreadIdForAddress(address: String): Long? {
        if (!hasReadPerms()) return null
        val projection = arrayOf(
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.DATE
        )
        val candidates = addressCandidates(address)
        candidates.forEach { candidate ->
            val cursor = runCatching {
                context.contentResolver.query(
                    Telephony.Sms.CONTENT_URI,
                    projection,
                    "${Telephony.Sms.ADDRESS}=?",
                    arrayOf(candidate),
                    "${Telephony.Sms.DATE} DESC"
                )
            }.getOrNull() ?: return@forEach
            cursor.use { c ->
                if (c.moveToFirst()) {
                    val idx = c.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
                    return c.getLong(idx)
                }
            }
        }
        return null
    }

    fun purgeExpiredOtpMessages(expiryMillis: Long) {
        if (!hasWritePerms()) return
        val cutoff = System.currentTimeMillis() - expiryMillis
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )
        val cursor = runCatching {
            context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                projection,
                "${Telephony.Sms.DATE}<?",
                arrayOf(cutoff.toString()),
                "${Telephony.Sms.DATE} ASC"
            )
        }.getOrNull() ?: return

        val staleIds = mutableListOf<Long>()
        cursor.use { c ->
            val idIdx = c.getColumnIndexOrThrow(Telephony.Sms._ID)
            val addrIdx = c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIdx = c.getColumnIndexOrThrow(Telephony.Sms.BODY)
            while (c.moveToNext() && staleIds.size < 200) {
                val addr = c.getString(addrIdx) ?: ""
                val body = c.getString(bodyIdx) ?: ""
                if (OtpHelper.isOtpMessage(addr, body)) {
                    staleIds += c.getLong(idIdx)
                }
            }
        }

        if (staleIds.isNotEmpty()) {
            staleIds.forEach { id ->
                context.contentResolver.delete(
                    Telephony.Sms.CONTENT_URI,
                    "${Telephony.Sms._ID}=?",
                    arrayOf(id.toString())
                )
            }
            observerFlow.tryEmit(Unit)
        }
    }

    private fun resolveAddress(raw: String?): String {
        if (!hasReadPerms()) return raw.orEmpty()
        val number = raw?.trim().orEmpty()
        if (number.isBlank()) return ""
        val uri = ContactsContract.PhoneLookup.CONTENT_FILTER_URI
        val lookupUri = Uri.withAppendedPath(uri, Uri.encode(number))
        runCatching {
            context.contentResolver.query(
                lookupUri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.PhoneLookup.NUMBER),
                null,
                null,
                null
            )
        }.getOrNull()?.use { c ->
            if (c.moveToFirst()) {
                val nameIdx = c.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME)
                val numIdx = c.getColumnIndexOrThrow(ContactsContract.PhoneLookup.NUMBER)
                val name = c.getString(nameIdx) ?: ""
                val formatted = c.getString(numIdx) ?: number
                return if (name.isNotBlank()) "$name · $formatted" else formatted
            }
        }
        return number
    }

    private fun addressCandidates(raw: String): List<String> {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return emptyList()
        val parsedNumber = when {
            trimmed.contains(" ú ") -> trimmed.split(" ú ", limit = 2).getOrNull(1) ?: trimmed
            trimmed.contains(" Ł ") -> trimmed.split(" Ł ", limit = 2).getOrNull(1) ?: trimmed
            trimmed.contains(" L ") -> trimmed.split(" L ", limit = 2).getOrNull(1) ?: trimmed
            else -> trimmed
        }.trim()
        val normalized = normalizePhone(parsedNumber)
        val noPlus = normalized.removePrefix("+")
        return listOf(trimmed, parsedNumber, normalized, noPlus)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    private fun normalizePhone(input: String): String {
        if (input.isBlank()) return ""
        val digits = buildString {
            input.forEach { ch ->
                if (ch.isDigit()) append(ch)
            }
        }
        return if (input.startsWith("+")) "+$digits" else digits
    }

    fun markThreadRead(threadId: Long): Boolean {
        if (!hasWritePerms()) return false
        val values = android.content.ContentValues().apply {
            put(Telephony.Sms.READ, 1)
            put(Telephony.Sms.SEEN, 1)
        }
        return runCatching {
            context.contentResolver.update(
                Telephony.Sms.CONTENT_URI,
                values,
                "${Telephony.Sms.THREAD_ID}=?",
                arrayOf(threadId.toString())
            )
            context.contentResolver.update(
                Telephony.Threads.CONTENT_URI,
                values,
                "${Telephony.Threads._ID}=?",
                arrayOf(threadId.toString())
            )
            observerFlow.tryEmit(Unit)
            true
        }.getOrDefault(false)
    }

    fun archiveThread(threadId: Long): Boolean {
        return runCatching {
            archivedThreadDao.insert(ArchivedThread(threadId = threadId, archivedAt = System.currentTimeMillis()))
            observerFlow.tryEmit(Unit)
            true
        }.getOrDefault(false)
    }

    fun unarchiveThread(threadId: Long): Boolean {
        return runCatching {
            archivedThreadDao.deleteByThreadId(threadId)
            observerFlow.tryEmit(Unit)
            true
        }.getOrDefault(false)
    }

    fun isThreadArchived(threadId: Long): Boolean {
        return runCatching { archivedThreadDao.isArchived(threadId) }.getOrDefault(false)
    }

    fun deleteThread(threadId: Long): Boolean {
        if (!hasWritePerms()) return false
        return runCatching {
            context.contentResolver.delete(
                Telephony.Sms.CONTENT_URI,
                "${Telephony.Sms.THREAD_ID}=?",
                arrayOf(threadId.toString())
            )
            archivedThreadDao.deleteByThreadId(threadId)
            observerFlow.tryEmit(Unit)
            true
        }.getOrDefault(false)
    }

    companion object {
        private fun isDefaultSmsApp(context: Context): Boolean {
            return runCatching {
                val roleHeld = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    context.getSystemService(RoleManager::class.java)
                        ?.isRoleHeld(RoleManager.ROLE_SMS) == true
                } else {
                    false
                }
                val telephonyDefault =
                    Telephony.Sms.getDefaultSmsPackage(context) == context.packageName
                roleHeld || telephonyDefault
            }.getOrDefault(false)
        }

        private fun hasReadSmsPermission(context: Context): Boolean {
            return ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_SMS
            ) == PackageManager.PERMISSION_GRANTED
        }

        private fun hasWriteSmsPermission(context: Context): Boolean {
            return isDefaultSmsApp(context)
        }
    }

    private fun hasReadPerms(): Boolean = isDefaultSmsApp(context) || hasReadSmsPermission(context)
    private fun hasWritePerms(): Boolean = hasWriteSmsPermission(context)

    private fun getLastValidMessageForThread(threadId: Long): SmsMessageItem? {
        val projection = arrayOf(
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE
        )
        val cursor = runCatching {
            context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                projection,
                "${Telephony.Sms.THREAD_ID}=?",
                arrayOf(threadId.toString()),
                "${Telephony.Sms.DATE} DESC"
            )
        }.getOrNull() ?: return null

        cursor.use { c ->
            val addrIdx = c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIdx = c.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIdx = c.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val typeIdx = c.getColumnIndexOrThrow(Telephony.Sms.TYPE)
            while (c.moveToNext()) {
                val body = c.getString(bodyIdx) ?: ""
                if (!SmsCodec.isPulseLinkPayload(body)) {
                    val address = c.getString(addrIdx) ?: ""
                    val ts = c.getLong(dateIdx)
                    val type = c.getInt(typeIdx)
                    val outgoing = type == Telephony.Sms.MESSAGE_TYPE_SENT || type == Telephony.Sms.MESSAGE_TYPE_OUTBOX
                    return SmsMessageItem(
                        id = -1, // Not needed for snippet
                        threadId = threadId,
                        address = address,
                        body = body,
                        timestamp = ts,
                        outgoing = outgoing
                    )
                }
            }
        }
        return null
    }

    private fun ensureObserversRegistered() {
        if (observersRegistered || !hasReadPerms()) return
        val handler = Handler(Looper.getMainLooper())
        val observer = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
                observerFlow.tryEmit(Unit)
            }
        }
        val registered = runCatching {
            context.contentResolver.registerContentObserver(
                Telephony.Sms.CONTENT_URI,
                true,
                observer
            )
            context.contentResolver.registerContentObserver(
                Telephony.Threads.CONTENT_URI,
                true,
                observer
            )
            true
        }.getOrDefault(false)
        observersRegistered = registered
    }

    private suspend fun listThreadsFromSms(
        limit: Int,
        archivedIds: List<Long>,
        includeArchived: Boolean,
        onlyArchived: Boolean
    ): List<SmsThreadItem> {
        if (!hasReadPerms()) return emptyList()
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.READ
        )
        val cursor = runCatching {
            context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                projection,
                null,
                null,
                "${Telephony.Sms.DATE} DESC"
            )
        }.getOrNull() ?: return emptyList()

        val archivedSet = archivedIds.toSet()
        cursor.use { c ->
            val threadIdx = c.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
            val addrIdx = c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIdx = c.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIdx = c.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val readIdx = c.getColumnIndexOrThrow(Telephony.Sms.READ)
            val seenThreads = HashSet<Long>()
            val items = mutableListOf<SmsThreadItem>()
            var count = 0
            while (c.moveToNext() && count < limit) {
                val threadId = c.getLong(threadIdx)
                val body = c.getString(bodyIdx) ?: ""
                if (SmsCodec.isPulseLinkPayload(body)) {
                    continue
                }
                if (!seenThreads.add(threadId)) continue
                val ts = c.getLong(dateIdx)
                val unread = c.getInt(readIdx) == 0
                val address = resolveAddress(c.getString(addrIdx))
                val isArchived = archivedSet.contains(threadId)

                if (onlyArchived && !isArchived) {
                    // skip
                } else if (!includeArchived && isArchived) {
                    // skip
                } else {
                    val parts = address.split(" ú ")
                    val phone = if (parts.size > 1) parts[1] else parts[0]
                    val normalized = normalizePhone(phone)
                    val contact = contactDao.getByPhone(phone)
                        ?: contactDao.getByPhone(normalized)
                    val trustedUrgency = contact?.let {
                        when {
                            OtpHelper.isUrgentBody(body) -> MessageUrgency.URGENT
                            it.escalationTier == EscalationTier.EMERGENCY -> MessageUrgency.EMERGENCY
                            else -> MessageUrgency.STANDARD
                        }
                    }
                    items += SmsThreadItem(
                        threadId = threadId,
                        address = address,
                        snippet = body,
                        timestamp = ts,
                        unread = unread,
                        isPrivate = contact?.isPrivate == true,
                        isFavorite = contact?.isFavorite == true,
                        isTrusted = contact != null,
                        trustedUrgency = trustedUrgency,
                        isOtp = OtpHelper.isOtpMessage(phone, body)
                    )
                }
                count++
            }
            return items
        }
    }
}
