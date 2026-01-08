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

        val uiItems = mutableListOf<ThreadUiItem>()
        var currentDay: LocalDate? = null

        // Iterate Newest -> Oldest
        for (i in messages.indices) {
            val msg = messages[i]
            val msgDate = Instant.ofEpochMilli(msg.timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()

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
