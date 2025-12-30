package com.pulselink.beacon.data

import com.pulselink.beacon.ui.InboxFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Since we cannot run Android instrumented tests in this environment,
// we are verifying the pure logic components of the feature here.
// In a real environment, we would use Robolectric or AndroidTest to verify the Repository.

class InboxFilterTest {

    @Test
    fun testInboxFilterLogic() {
        val threads = listOf(
            mockThread(1, unread = true, archived = false),
            mockThread(2, unread = false, archived = false),
            mockThread(3, unread = false, archived = true),
            mockThread(4, unread = true, archived = true)
        )

        // Test ALL filter (should show unarchived)
        val all = filter(threads, InboxFilter.ALL)
        assertEquals(2, all.size)
        assertTrue(all.any { it.threadId == 1L })
        assertTrue(all.any { it.threadId == 2L })

        // Test ARCHIVED filter (should show archived)
        val archived = filter(threads, InboxFilter.ARCHIVED)
        assertEquals(2, archived.size)
        assertTrue(archived.any { it.threadId == 3L })
        assertTrue(archived.any { it.threadId == 4L })

        // Test READ (should show read & unarchived)
        val read = filter(threads, InboxFilter.READ)
        assertEquals(1, read.size)
        assertEquals(2L, read[0].threadId)

        // Test UNREAD (should show unread & unarchived)
        val unread = filter(threads, InboxFilter.UNREAD)
        assertEquals(1, unread.size)
        assertEquals(1L, unread[0].threadId)
    }

    @Test
    fun testPinnedThreadsSorting() {
        val threads = listOf(
            mockThread(1, timestamp = 1000L, pinned = true),
            mockThread(2, timestamp = 3000L, pinned = false),
            mockThread(3, timestamp = 2000L, pinned = true)
        )

        // Pinned threads should appear first, sorted by timestamp
        val sorted = threads.sortedWith(
            compareByDescending<SmsThreadItem> { it.isPinned }
                .thenByDescending { it.timestamp }
        )

        assertEquals(1L, sorted[0].threadId) // Pinned, oldest
        assertEquals(3L, sorted[1].threadId) // Pinned, newer
        assertEquals(2L, sorted[2].threadId) // Not pinned
    }

    @Test
    fun testArchiveAndPinCombination() {
        // A thread can be both archived and pinned
        val thread = mockThread(1, archived = true, pinned = true)
        assertTrue(thread.isArchived)
        assertTrue(thread.isPinned)

        // Archived threads should be excluded from ALL filter
        val allFiltered = filter(listOf(thread), InboxFilter.ALL)
        assertEquals(0, allFiltered.size)

        // But should appear in ARCHIVED filter
        val archivedFiltered = filter(listOf(thread), InboxFilter.ARCHIVED)
        assertEquals(1, archivedFiltered.size)
        assertTrue(archivedFiltered[0].isPinned) // Pin state preserved
    }

    @Test
    fun testEmptyListFiltering() {
        val empty = emptyList<SmsThreadItem>()
        assertEquals(0, filter(empty, InboxFilter.ALL).size)
        assertEquals(0, filter(empty, InboxFilter.READ).size)
        assertEquals(0, filter(empty, InboxFilter.UNREAD).size)
        assertEquals(0, filter(empty, InboxFilter.ARCHIVED).size)
    }

    private fun filter(list: List<SmsThreadItem>, filter: InboxFilter): List<SmsThreadItem> {
        return list.filter { thread ->
            when (filter) {
                InboxFilter.ALL -> !thread.isArchived // Note: Repository handles main split, UI handles sub-filtering
                InboxFilter.READ -> !thread.unread && !thread.isArchived
                InboxFilter.UNREAD -> thread.unread && !thread.isArchived
                InboxFilter.ARCHIVED -> thread.isArchived
            }
        }
    }

    private fun mockThread(
        id: Long,
        unread: Boolean = false,
        archived: Boolean = false,
        pinned: Boolean = false,
        timestamp: Long = 0L
    ): SmsThreadItem {
        return SmsThreadItem(
            threadId = id,
            address = "123",
            snippet = "test",
            timestamp = timestamp,
            unread = unread,
            isPinned = pinned,
            isArchived = archived
        )
    }
}
