package com.pulselink.data.sms

import android.content.Context
import android.provider.Telephony
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import com.pulselink.data.db.ArchivedThreadDao
import com.pulselink.domain.model.ArchivedThread
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import android.provider.ContactsContract
import android.net.Uri

@Singleton
class SmsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val archivedThreadDao: ArchivedThreadDao
) {

    private val hasPerms = hasSmsPermissions(context)
    private val observerFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    init {
        if (hasPerms) {
            val handler = Handler(Looper.getMainLooper())
            val observer = object : ContentObserver(handler) {
                override fun onChange(selfChange: Boolean) {
                    observerFlow.tryEmit(Unit)
                }
            }
            runCatching {
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
            }
        }
    }

    fun changes(): SharedFlow<Unit> = observerFlow.asSharedFlow()

    fun listThreads(limit: Int = 50, includeArchived: Boolean = false, onlyArchived: Boolean = false): List<SmsThreadItem> {
        if (!hasPerms) return emptyList()
        val archivedIds = runCatching { archivedThreadDao.getAllIds() }.getOrDefault(emptyList())
        val projection = arrayOf(
            Telephony.Threads._ID,
            Telephony.Threads.SNIPPET,
            Telephony.Threads.DATE,
            Telephony.Threads.RECIPIENT_IDS,
            Telephony.Threads.MESSAGE_COUNT,
            Telephony.Threads.READ
        )
        val cursor = context.contentResolver.query(
            Telephony.Threads.CONTENT_URI,
            projection,
            null,
            null,
            "${Telephony.Threads.DATE} DESC"
        )

        cursor ?: return emptyList()
        cursor.use { c ->
            val idIdx = c.getColumnIndexOrThrow(Telephony.Threads._ID)
            val snippetIdx = c.getColumnIndexOrThrow(Telephony.Threads.SNIPPET)
            val dateIdx = c.getColumnIndexOrThrow(Telephony.Threads.DATE)
            val readIdx = c.getColumnIndexOrThrow(Telephony.Threads.READ)
            val addressIdx = c.getColumnIndexOrThrow(Telephony.Threads.RECIPIENT_IDS)
            val items = mutableListOf<SmsThreadItem>()
            var count = 0
            while (c.moveToNext() && count < limit) {
                val threadId = c.getLong(idIdx)
                val snippet = c.getString(snippetIdx) ?: ""
                val ts = c.getLong(dateIdx)
                val unread = c.getInt(readIdx) == 0
                val address = resolveAddress(c.getString(addressIdx))
                val isArchived = archivedIds.contains(threadId)
                if (onlyArchived && !isArchived) {
                    // skip
                } else if (!includeArchived && isArchived) {
                    // skip
                } else {
                    items += SmsThreadItem(
                        threadId = threadId,
                        address = address,
                        snippet = snippet,
                        timestamp = ts,
                        unread = unread
                    )
                }
                count++
            }
            return items
        }
    }

    fun listArchivedThreads(limit: Int = 50): List<SmsThreadItem> =
        listThreads(limit = limit, includeArchived = true, onlyArchived = true)

    fun messagesForThread(threadId: Long, limit: Int = 200): List<SmsMessageItem> {
        if (!hasPerms) return emptyList()
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE
        )
        val cursor = context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            projection,
            "${Telephony.Sms.THREAD_ID}=?",
            arrayOf(threadId.toString()),
            "${Telephony.Sms.DATE} DESC"
        )
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

    private fun resolveAddress(raw: String?): String {
        if (!hasPerms) return raw.orEmpty()
        val number = raw?.trim().orEmpty()
        if (number.isBlank()) return ""
        val uri = ContactsContract.PhoneLookup.CONTENT_FILTER_URI
        val lookupUri = Uri.withAppendedPath(uri, Uri.encode(number))
        context.contentResolver.query(
            lookupUri,
            arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.PhoneLookup.NUMBER),
            null,
            null,
            null
        )?.use { c ->
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

    fun markThreadRead(threadId: Long): Boolean {
        if (!hasPerms) return false
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

    fun deleteThread(threadId: Long): Boolean {
        if (!hasPerms) return false
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
        private fun hasSmsPermissions(context: Context): Boolean {
            val perms = listOf(
                android.Manifest.permission.READ_SMS,
                android.Manifest.permission.RECEIVE_SMS,
                android.Manifest.permission.SEND_SMS,
                android.Manifest.permission.RECEIVE_MMS,
                android.Manifest.permission.RECEIVE_WAP_PUSH
            )
            return perms.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        }
    }
}
