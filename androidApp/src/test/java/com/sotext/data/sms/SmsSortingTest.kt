package com.sotext.data.sms

import org.junit.Test
import org.junit.Assert.*

class SmsSortingTest {

    @Test
    fun `verify pinned threads come first`() {
        val t1 = SmsThreadItem(
            threadId = 1,
            address = "123",
            snippet = "Hello",
            timestamp = 1000L,
            unread = false,
            isPinned = false
        )
        val t2 = SmsThreadItem(
            threadId = 2,
            address = "456",
            snippet = "World",
            timestamp = 2000L, // Newer than t1
            unread = false,
            isPinned = true // Pinned
        )
        val t3 = SmsThreadItem(
            threadId = 3,
            address = "789",
            snippet = "Test",
            timestamp = 3000L, // Newest
            unread = false,
            isPinned = false
        )

        val items = listOf(t1, t2, t3)
        val sorted = items.sortedWith(
            compareByDescending<SmsThreadItem> { it.isPinned }
                .thenByDescending { it.timestamp }
        )

        // Expected order:
        // 1. t2 (Pinned, regardless of timestamp)
        // 2. t3 (Not pinned, newest)
        // 3. t1 (Not pinned, oldest)

        assertEquals(2L, sorted[0].threadId)
        assertEquals(3L, sorted[1].threadId)
        assertEquals(1L, sorted[2].threadId)
    }
}
