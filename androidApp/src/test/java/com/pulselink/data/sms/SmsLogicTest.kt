package com.pulselink.data.sms

import org.junit.Test
import org.junit.Assert.*

class SmsLogicTest {

    data class MockRow(val threadId: Long, val body: String)

    @Test
    fun `reproduce hidden thread bug`() {
        // Setup: Thread 1 has a newest message that is a payload, and an older message that is normal.
        val rows = listOf(
            MockRow(1, "PULSELINK|PING|123"), // Newest
            MockRow(1, "Hello world"),        // Older
            MockRow(2, "Hi there")            // Another thread
        )

        // Current Logic Simulation
        val seenThreads = HashSet<Long>()
        val items = mutableListOf<Long>() // List of threadIds

        for (row in rows) {
            val threadId = row.threadId

            // Logic from SmsRepository.listThreadsFromSms
            if (!seenThreads.add(threadId)) continue // Mark as seen
            val body = row.body
            if (body.startsWith("PULSELINK")) {
                continue // Skip payload
            }

            items.add(threadId)
        }

        // Assert that thread 1 is MISSING (Bug reproduction)
        assertFalse("Thread 1 should be missing due to bug", items.contains(1L))
        assertTrue("Thread 2 should be present", items.contains(2L))
    }

    @Test
    fun `verify fix logic`() {
        // Setup: Same data
        val rows = listOf(
            MockRow(1, "PULSELINK|PING|123"), // Newest
            MockRow(1, "Hello world"),        // Older
            MockRow(2, "Hi there")            // Another thread
        )

        // Fix Logic Simulation
        val seenThreads = HashSet<Long>()
        val items = mutableListOf<Long>()

        for (row in rows) {
            val threadId = row.threadId
            val body = row.body

            // Fix: Check payload first
            if (body.startsWith("PULSELINK")) {
                continue // Skip payload, do NOT mark as seen yet
            }

            // Then check seen
            if (!seenThreads.add(threadId)) continue

            items.add(threadId)
        }

        // Assert that thread 1 is PRESENT (Fix verification)
        assertTrue("Thread 1 should be present", items.contains(1L))
        assertTrue("Thread 2 should be present", items.contains(2L))
    }
}
