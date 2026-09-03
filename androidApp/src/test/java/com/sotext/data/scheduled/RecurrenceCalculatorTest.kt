package com.sotext.data.scheduled

import com.sotext.domain.model.RecurrenceFrequency
import com.sotext.domain.model.RecurrenceRule
import org.junit.Assert.*
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class RecurrenceCalculatorTest {

    private val ny = "America/New_York"
    private val nyZone = ZoneId.of(ny)

    private fun millisAt(year: Int, month: Int, day: Int, hour: Int, minute: Int, zoneId: String = ny): Long =
        ZonedDateTime.of(year, month, day, hour, minute, 0, 0, ZoneId.of(zoneId)).toInstant().toEpochMilli()

    private fun localOf(millis: Long, zoneId: String = ny): ZonedDateTime =
        Instant.ofEpochMilli(millis).atZone(ZoneId.of(zoneId))

    @Test
    fun `daily recurrence advances exactly one day at the same local time`() {
        val after = millisAt(2026, 6, 1, 9, 0)
        val next = RecurrenceCalculator.nextOccurrence(
            RecurrenceRule(frequency = RecurrenceFrequency.DAILY), after, ny
        )
        assertNotNull(next)
        val local = localOf(next!!)
        assertEquals(2026, local.year)
        assertEquals(6, local.monthValue)
        assertEquals(2, local.dayOfMonth)
        assertEquals(9, local.hour)
        assertEquals(0, local.minute)
    }

    @Test
    fun `weekly with no explicit days simply adds one interval of weeks`() {
        val after = millisAt(2026, 6, 1, 9, 0) // a Monday
        val next = RecurrenceCalculator.nextOccurrence(
            RecurrenceRule(frequency = RecurrenceFrequency.WEEKLY), after, ny
        )
        assertNotNull(next)
        assertEquals(DayOfWeek.MONDAY, localOf(next!!).dayOfWeek)
        assertEquals(8, localOf(next).dayOfMonth)
    }

    @Test
    fun `weekly with explicit days lands on the next matching weekday`() {
        val after = millisAt(2026, 6, 1, 9, 0) // Monday
        val next = RecurrenceCalculator.nextOccurrence(
            RecurrenceRule(frequency = RecurrenceFrequency.WEEKLY, daysOfWeek = setOf(3, 5)), // Wed, Fri
            after,
            ny
        )
        assertNotNull(next)
        assertEquals(DayOfWeek.WEDNESDAY, localOf(next!!).dayOfWeek)
    }

    @Test
    fun `monthly on Jan 31 clamps to Feb 28 in a non-leap year`() {
        val after = millisAt(2026, 1, 31, 9, 0)
        val next = RecurrenceCalculator.nextOccurrence(
            RecurrenceRule(frequency = RecurrenceFrequency.MONTHLY, dayOfMonth = 31), after, ny
        )
        assertNotNull(next)
        val local = localOf(next!!)
        assertEquals(2, local.monthValue)
        assertEquals(28, local.dayOfMonth)
    }

    @Test
    fun `monthly on Jan 31 lands on Feb 29 in a leap year`() {
        val after = millisAt(2024, 1, 31, 9, 0)
        val next = RecurrenceCalculator.nextOccurrence(
            RecurrenceRule(frequency = RecurrenceFrequency.MONTHLY, dayOfMonth = 31), after, ny
        )
        assertNotNull(next)
        val local = localOf(next!!)
        assertEquals(2, local.monthValue)
        assertEquals(29, local.dayOfMonth)
    }

    @Test
    fun `monthly on the 31st clamps to Apr 30`() {
        val after = millisAt(2026, 3, 31, 9, 0)
        val next = RecurrenceCalculator.nextOccurrence(
            RecurrenceRule(frequency = RecurrenceFrequency.MONTHLY, dayOfMonth = 31), after, ny
        )
        assertNotNull(next)
        val local = localOf(next!!)
        assertEquals(4, local.monthValue)
        assertEquals(30, local.dayOfMonth)
    }

    @Test
    fun `yearly on Feb 29 clamps to Feb 28 on a non-leap target year`() {
        val after = millisAt(2024, 2, 29, 9, 0) // 2024 is a leap year
        val next = RecurrenceCalculator.nextOccurrence(
            RecurrenceRule(frequency = RecurrenceFrequency.YEARLY), after, ny
        )
        assertNotNull(next)
        val local = localOf(next!!)
        assertEquals(2025, local.year)
        assertEquals(2, local.monthValue)
        assertEquals(28, local.dayOfMonth)
    }

    @Test
    fun `series with an end date stops once the next occurrence would be past it`() {
        val after = millisAt(2026, 6, 1, 9, 0)
        val endDate = millisAt(2026, 6, 3, 0, 0) // before the next daily occurrence (Jun 2, 9am is fine; Jun 3 cutoff still allows it)
        val rule = RecurrenceRule(frequency = RecurrenceFrequency.DAILY, endDateUtcMillis = endDate)
        val next = RecurrenceCalculator.nextOccurrence(rule, after, ny)
        assertNotNull("Jun 2 9am is still before the Jun 3 cutoff", next)

        val afterCutoff = millisAt(2026, 6, 3, 9, 0)
        val stopped = RecurrenceCalculator.nextOccurrence(rule, afterCutoff, ny)
        assertNull("Jun 4 9am is past the Jun 3 cutoff", stopped)
    }

    @Test
    fun `series with an occurrence count stops once reached`() {
        val after = millisAt(2026, 6, 1, 9, 0)
        val rule = RecurrenceRule(frequency = RecurrenceFrequency.DAILY, occurrenceCount = 3)
        assertNotNull(RecurrenceCalculator.nextOccurrence(rule, after, ny, occurrenceIndex = 2))
        assertNull(RecurrenceCalculator.nextOccurrence(rule, after, ny, occurrenceIndex = 3))
    }

    @Test
    fun `daily recurrence across spring-forward keeps advancing without crashing`() {
        // US spring-forward 2026: clocks jump from 2:00 AM to 3:00 AM on Sunday, March 8.
        val beforeTransition = millisAt(2026, 3, 7, 2, 30)
        val next = RecurrenceCalculator.nextOccurrence(
            RecurrenceRule(frequency = RecurrenceFrequency.DAILY), beforeTransition, ny
        )
        assertNotNull(next)
        val local = localOf(next!!)
        assertEquals(8, local.dayOfMonth)
        // 2:30 AM doesn't exist on the transition day - java.time resolves the gap by shifting
        // forward, landing at 3:30 AM local rather than throwing or silently losing the day.
        assertEquals(3, local.hour)
        assertEquals(30, local.minute)
    }

    @Test
    fun `daily recurrence across fall-back resolves to a single well-defined instant`() {
        // US fall-back 2026: clocks repeat 1:00-2:00 AM on Sunday, November 1.
        val beforeTransition = millisAt(2026, 10, 31, 1, 30)
        val next = RecurrenceCalculator.nextOccurrence(
            RecurrenceRule(frequency = RecurrenceFrequency.DAILY), beforeTransition, ny
        )
        assertNotNull(next)
        val local = localOf(next!!)
        assertEquals(11, local.monthValue)
        assertEquals(1, local.dayOfMonth)
        assertEquals(1, local.hour)
        assertEquals(30, local.minute)
        // Whichever of the two 1:30 AM instants java.time resolves to, it must be exactly one -
        // no duplicate/skipped fire across the repeated hour.
    }
}
