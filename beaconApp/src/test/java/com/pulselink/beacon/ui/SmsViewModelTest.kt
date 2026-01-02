package com.pulselink.beacon.ui

import org.junit.Test
import org.junit.Assert.*
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class SmsViewModelTest {

    @Test
    fun testTimezoneCalculation() {
        // Mock a selected date (UTC midnight)
        // e.g., 2024-01-01 00:00:00 UTC
        val utcDateMillis = 1704067200000L

        // Selected time: 10:30 AM
        val hour = 10
        val minute = 30

        // Assume system zone is UTC-5 (EST)
        val zoneId = ZoneId.of("America/New_York")

        val utcDate = Instant.ofEpochMilli(utcDateMillis)
            .atZone(ZoneId.of("UTC"))
            .toLocalDate() // 2024-01-01

        val localTime = LocalTime.of(hour, minute)
        val targetZoned = utcDate.atTime(localTime).atZone(zoneId)

        // Expected: 2024-01-01 10:30:00 EST
        // In UTC: 2024-01-01 15:30:00 UTC
        val expectedEpoch = 1704123000000L // 1704067200000 + 15.5 * 3600 * 1000

        assertEquals(expectedEpoch, targetZoned.toInstant().toEpochMilli())
    }
}
