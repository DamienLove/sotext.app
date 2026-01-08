package com.pulselink.beacon.util

import android.text.format.DateUtils
import com.pulselink.beacon.data.SmsMessageItem
import com.pulselink.beacon.ui.ThreadUiItem
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object ThreadDateUtils {

    fun mapMessagesToUi(messages: List<SmsMessageItem>): List<ThreadUiItem> {
        if (messages.isEmpty()) return emptyList()

        // Input messages are sorted NEWEST -> OLDEST (Desc).
        // UI uses reverseLayout = true (Bottom -> Top).
        // Index 0 in List -> Bottom of Screen.

        // We want headers to appear ABOVE the messages of that day.
        // In reverseLayout (Bottom-Up rendering):
        // [Item 0 (Newest Msg)]
        // ...
        // [Item K (Oldest Msg of Today)]
        // [Item K+1 (Date Header Today)] -> Renders Above K
        // [Item K+2 (Newest Msg of Yesterday)]

        // Algorithm: Iterate through messages. Track current day.
        // When day changes (from Today to Yesterday), insert Header for the *previous* group (Today).

        // Pre-allocate capacity: messages + potential date headers (estimate ~10% of messages)
        val estimatedCapacity = messages.size + (messages.size / 10).coerceAtLeast(1)
        val uiItems = ArrayList<ThreadUiItem>(estimatedCapacity)
        var currentDay: LocalDate? = null

        // Iterate Newest -> Oldest
        for (i in messages.indices) {
            val msg = messages[i]

            // Validate timestamp (reject negative or far future timestamps)
            if (msg.timestamp < 0 || msg.timestamp > System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000) {
                // Skip invalid timestamp, or use a default
                continue
            }

            val msgDate = runCatching {
                Instant.ofEpochMilli(msg.timestamp)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
            }.getOrElse {
                // If date parsing fails, skip this message
                continue
            }

            // If it's the first item, set current day
            if (currentDay == null) {
                currentDay = msgDate
            }

            // Check if day changed
            if (msgDate != currentDay) {
                // We moved from Day A to Day B (older).
                // Insert header for Day A.
                uiItems.add(ThreadUiItem.DateHeader(formatDate(currentDay!!)))
                currentDay = msgDate
            }

            uiItems.add(ThreadUiItem.Message(msg))
        }

        // Add final header for the oldest group
        if (currentDay != null) {
            uiItems.add(ThreadUiItem.DateHeader(formatDate(currentDay!!)))
        }

        return uiItems
    }

    private fun formatDate(date: LocalDate): String {
        val now = LocalDate.now()
        return when {
            date.isEqual(now) -> "Today"
            date.isEqual(now.minusDays(1)) -> "Yesterday"
            date.year == now.year -> date.format(DateTimeFormatter.ofPattern("MMM d"))
            else -> date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
        }
    }
}
