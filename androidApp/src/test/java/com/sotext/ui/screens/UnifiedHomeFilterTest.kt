package com.sotext.ui.screens

import com.sotext.data.sms.SmsThreadItem
import com.sotext.util.normalizeSmsAddress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UnifiedHomeFilterTest {

    @Test
    fun `line scoping keeps default line and null line threads`() {
        val threads = listOf(
            thread(id = 1, address = "111", lineId = null),
            thread(id = 2, address = "222", lineId = "L1"),
            thread(id = 3, address = "333", lineId = "L2")
        )

        val filtered = previewThreads(
            threads = threads,
            isPremium = true,
            orderedLinesPresent = true,
            activeLineId = "L1",
            deviceLineId = null,
            filter = InboxFilter.ALL,
            contacts = emptySet()
        )

        assertEquals(listOf(threads[0], threads[1]), filtered)
    }

    @Test
    fun `trusted filter keeps null-line threads and active line`() {
        val threads = listOf(
            thread(id = 1, address = "111", lineId = null, isTrusted = true),
            thread(id = 2, address = "222", lineId = "L2", isTrusted = true),
            thread(id = 3, address = "333", lineId = "L2", isTrusted = false)
        )

        val filtered = previewThreads(
            threads = threads,
            isPremium = true,
            orderedLinesPresent = true,
            activeLineId = "L2",
            deviceLineId = null,
            filter = InboxFilter.TRUSTED,
            contacts = emptySet()
        )

        assertEquals(listOf(threads[0], threads[1]), filtered)
    }

    @Test
    fun `archived filter is empty in preview`() {
        val filtered = previewThreads(
            threads = listOf(thread(id = 1, address = "111")),
            isPremium = false,
            orderedLinesPresent = false,
            activeLineId = null,
            deviceLineId = null,
            filter = InboxFilter.ARCHIVED,
            contacts = emptySet()
        )

        assertTrue(filtered.isEmpty())
    }

    @Test
    fun `preview caps at eight threads`() {
        val threads = (1..10).map { thread(id = it.toLong(), address = "$it") }

        val filtered = previewThreads(
            threads = threads,
            isPremium = false,
            orderedLinesPresent = false,
            activeLineId = null,
            deviceLineId = null,
            filter = InboxFilter.ALL,
            contacts = emptySet()
        )

        assertEquals(8, filtered.size)
        assertEquals(threads.take(8), filtered)
    }

    private fun thread(
        id: Long,
        address: String,
        lineId: String? = null,
        isTrusted: Boolean = false
    ): SmsThreadItem = SmsThreadItem(
        threadId = id,
        address = address,
        snippet = "",
        timestamp = 0L,
        unread = false,
        unreadCount = 0,
        isPrivate = false,
        isFavorite = false,
        isTrusted = isTrusted,
        trustedUrgency = null,
        isOtp = false,
        lineId = lineId
    )

    private fun previewThreads(
        threads: List<SmsThreadItem>,
        isPremium: Boolean,
        orderedLinesPresent: Boolean,
        activeLineId: String?,
        deviceLineId: String?,
        filter: InboxFilter,
        contacts: Set<String>
    ): List<SmsThreadItem> {
        val lineFiltered = if (isPremium && orderedLinesPresent) {
            val effectiveActive = activeLineId ?: deviceLineId
            if (effectiveActive != null) {
                threads.filter { it.lineId.isNullOrBlank() || it.lineId == effectiveActive }
            } else {
                threads
            }
        } else {
            threads
        }

        val filteredByFilter = when (filter) {
            InboxFilter.ALL -> lineFiltered
            InboxFilter.OTP -> lineFiltered.filter { it.isOtp }
            InboxFilter.TRUSTED -> lineFiltered.filter { it.isTrusted }
            InboxFilter.FAVORITES -> lineFiltered.filter { it.isFavorite }
            InboxFilter.PRIVATE -> lineFiltered.filter { it.isPrivate }
            InboxFilter.CONTACTS -> lineFiltered.filter { contacts.contains(normalizeSmsAddress(it.address)) }
            InboxFilter.READ -> lineFiltered.filter { !it.unread }
            InboxFilter.UNREAD -> lineFiltered.filter { it.unread }
            InboxFilter.ARCHIVED -> emptyList()
        }

        return filteredByFilter.take(8)
    }
}
