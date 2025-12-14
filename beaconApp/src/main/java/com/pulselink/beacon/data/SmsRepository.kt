package com.pulselink.beacon.data

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.provider.Telephony
import com.pulselink.beacon.data.MmsPart
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class SmsRepository(private val context: Context) {

    private val observerFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    init {
        val handler = Handler(Looper.getMainLooper())
        val observer = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
                observerFlow.tryEmit(Unit)
            }
        }
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

    fun changes(): SharedFlow<Unit> = observerFlow.asSharedFlow()

    fun listThreads(limit: Int = 50): List<SmsThreadItem> {
        val projection = arrayOf(
            Telephony.Threads._ID,
            Telephony.Threads.SNIPPET,
            Telephony.Threads.DATE,
            Telephony.Threads.RECIPIENT_IDS,
            Telephony.Threads.READ
        )
        val cursor = context.contentResolver.query(
            Telephony.Threads.CONTENT_URI,
            projection,
            null,
            null,
            "${Telephony.Threads.DATE} DESC"
        ) ?: return emptyList()

        cursor.use { c ->
            val idIdx = c.getColumnIndexOrThrow(Telephony.Threads._ID)
            val snippetIdx = c.getColumnIndexOrThrow(Telephony.Threads.SNIPPET)
            val dateIdx = c.getColumnIndexOrThrow(Telephony.Threads.DATE)
            val readIdx = c.getColumnIndexOrThrow(Telephony.Threads.READ)
            val items = mutableListOf<SmsThreadItem>()
            var count = 0
            while (c.moveToNext() && count < limit) {
                val threadId = c.getLong(idIdx)
                val snippet = c.getString(snippetIdx) ?: ""
                val ts = c.getLong(dateIdx)
                val unread = c.getInt(readIdx) == 0
                val address = resolveThreadAddress(threadId)
                items += SmsThreadItem(
                    threadId = threadId,
                    address = address,
                    snippet = snippet,
                    timestamp = ts,
                    unread = unread
                )
                count++
            }
            return items
        }
    }

    private fun resolveThreadAddress(threadId: Long): String {
        val cursor = context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(Telephony.Sms.ADDRESS),
            "${Telephony.Sms.THREAD_ID}=?",
            arrayOf(threadId.toString()),
            "${Telephony.Sms.DATE} DESC"
        ) ?: return ""

        cursor.use { c ->
            if (c.moveToFirst()) {
                val addr = c.getString(c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS))
                return resolveAddress(addr)
            }
        }
        return ""
    }

    private fun readMmsMessages(threadId: Long, limit: Int): List<SmsMessageItem> {
        val projection = arrayOf(
            Telephony.Mms._ID,
            Telephony.Mms.THREAD_ID,
            Telephony.Mms.DATE,
            Telephony.Mms.MESSAGE_BOX
        )
        val cursor = context.contentResolver.query(
            Telephony.Mms.CONTENT_URI,
            projection,
            "${Telephony.Mms.THREAD_ID}=?",
            arrayOf(threadId.toString()),
            "${Telephony.Mms.DATE} DESC"
        ) ?: return emptyList()

        cursor.use { c ->
            val idIdx = c.getColumnIndexOrThrow(Telephony.Mms._ID)
            val dateIdx = c.getColumnIndexOrThrow(Telephony.Mms.DATE)
            val boxIdx = c.getColumnIndexOrThrow(Telephony.Mms.MESSAGE_BOX)
            val items = mutableListOf<SmsMessageItem>()
            var count = 0
            while (c.moveToNext() && count < limit) {
                val mmsId = c.getLong(idIdx)
                val address = resolveMmsAddress(mmsId)
                val parts = readMmsParts(mmsId)
                val textPart = parts.firstOrNull { it.text != null }?.text ?: "[MMS]"
                val ts = c.getLong(dateIdx) * 1000 // Mms dates are in seconds
                val msgBox = c.getInt(boxIdx)
                val outgoing = msgBox == Telephony.Mms.MESSAGE_BOX_SENT || msgBox == Telephony.Mms.MESSAGE_BOX_OUTBOX
                items += SmsMessageItem(
                    id = -mmsId, // avoid collision with SMS ids
                    threadId = threadId,
                    address = address,
                    body = textPart,
                    timestamp = ts,
                    outgoing = outgoing,
                    isMms = true,
                    mediaParts = parts
                )
                count++
            }
            return items
        }
    }

    private fun resolveMmsAddress(mmsId: Long): String {
        val uri = Uri.parse("content://mms/$mmsId/addr")
        val cursor = context.contentResolver.query(
            uri,
            arrayOf("address", "type"),
            "type=137", // FROM
            null,
            null
        ) ?: return ""
        cursor.use { c ->
            if (c.moveToFirst()) {
                val addr = c.getString(c.getColumnIndexOrThrow("address"))
                return resolveAddress(addr)
            }
        }
        return ""
    }

    private fun readMmsParts(mmsId: Long): List<MmsPart> {
        val uri = Uri.parse("content://mms/$mmsId/part")
        val cursor = context.contentResolver.query(
            uri,
            arrayOf("_id", "ct", "text"),
            null,
            null,
            null
        ) ?: return emptyList()

        val parts = mutableListOf<MmsPart>()
        cursor.use { c ->
            val idIdx = c.getColumnIndexOrThrow("_id")
            val ctIdx = c.getColumnIndexOrThrow("ct")
            val textIdx = c.getColumnIndexOrThrow("text")
            while (c.moveToNext()) {
                val partId = c.getString(idIdx)
                val contentType = c.getString(ctIdx) ?: ""
                val text = c.getString(textIdx)
                val dataUri = Uri.parse("content://mms/part/$partId")
                parts += MmsPart(
                    contentType = contentType,
                    text = text,
                    dataUri = if (text == null) dataUri else null
                )
            }
        }
        return parts
    }

    fun messagesForThread(threadId: Long, limit: Int = 200): List<SmsMessageItem> {
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE
        )
        val smsCursor = context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            projection,
            "${Telephony.Sms.THREAD_ID}=?",
            arrayOf(threadId.toString()),
            "${Telephony.Sms.DATE} DESC"
        )

        val smsItems = smsCursor?.use { c ->
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
                    outgoing = outgoing,
                    isMms = false
                )
                count++
            }
            items
        } ?: emptyList()

        val mmsItems = readMmsMessages(threadId, limit)

        return (smsItems + mmsItems).sortedBy { it.timestamp }.takeLast(limit)
    }

    fun sendSms(address: String, body: String): Boolean {
        return runCatching {
            val smsManager = android.telephony.SmsManager.getDefault()
            smsManager.sendTextMessage(address, null, body, null, null)
            true
        }.getOrDefault(false)
    }

    fun markThreadRead(threadId: Long) {
        val values = android.content.ContentValues().apply {
            put(Telephony.Sms.READ, 1)
            put(Telephony.Sms.SEEN, 1)
        }
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
    }

    fun deleteThread(threadId: Long) {
        context.contentResolver.delete(
            Telephony.Sms.CONTENT_URI,
            "${Telephony.Sms.THREAD_ID}=?",
            arrayOf(threadId.toString())
        )
        observerFlow.tryEmit(Unit)
    }

    private fun resolveAddress(raw: String?): String {
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
                return if (name.isNotBlank()) "$name \u2022 $formatted" else formatted
            }
        }
        return number
    }
}
