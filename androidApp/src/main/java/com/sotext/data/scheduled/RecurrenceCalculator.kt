package com.sotext.data.scheduled

import com.sotext.domain.model.RecurrenceFrequency
import com.sotext.domain.model.RecurrenceRule
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Pure recurrence math for scheduled messages. Uses [java.time.ZonedDateTime] exclusively, which
 * makes DST handling correct "for free": `plusDays`/`plusWeeks`/`plusMonths`/`plusYears` operate
 * on the local wall-clock date/time and let the [ZoneId] resolve the correct UTC offset for that
 * instant, so a 9:00 AM daily message stays 9:00 AM local straight through a spring-forward or
 * fall-back transition (the epoch-millis result shifts by the DST delta on the transition day;
 * the local time the user sees does not).
 */
object RecurrenceCalculator {

    /**
     * Returns the next occurrence strictly after [afterUtcMillis], or null if the series has
     * ended (past [RecurrenceRule.endDateUtcMillis], or [occurrenceIndex] has reached
     * [RecurrenceRule.occurrenceCount]).
     *
     * @param occurrenceIndex how many occurrences have already fired (0 for the first repeat
     *   after the original send), used against [RecurrenceRule.occurrenceCount].
     */
    fun nextOccurrence(
        rule: RecurrenceRule,
        afterUtcMillis: Long,
        zoneId: String,
        occurrenceIndex: Int = 0
    ): Long? {
        if (rule.occurrenceCount != null && occurrenceIndex >= rule.occurrenceCount) return null

        val zone = runCatching { ZoneId.of(zoneId) }.getOrDefault(ZoneId.systemDefault())
        val after = Instant.ofEpochMilli(afterUtcMillis).atZone(zone)
        val interval = rule.interval.coerceAtLeast(1)

        val next = when (rule.frequency) {
            RecurrenceFrequency.DAILY -> after.plusDays(interval.toLong())
            RecurrenceFrequency.WEEKLY -> nextWeekly(after, rule, interval)
            RecurrenceFrequency.CUSTOM -> nextWeekly(after, rule, interval)
            RecurrenceFrequency.MONTHLY -> nextMonthlyClamped(after, rule, interval)
            RecurrenceFrequency.YEARLY -> nextYearlyClamped(after, interval)
        }

        val nextMillis = next.toInstant().toEpochMilli()
        if (rule.endDateUtcMillis != null && nextMillis > rule.endDateUtcMillis) return null
        return nextMillis
    }

    /**
     * Weekly/custom: if [RecurrenceRule.daysOfWeek] is set, advances day-by-day to the next
     * matching weekday (still respecting [interval] as "every N weeks" once a full week has
     * elapsed since [after]); otherwise simply adds [interval] weeks.
     */
    private fun nextWeekly(after: ZonedDateTime, rule: RecurrenceRule, interval: Int): ZonedDateTime {
        if (rule.daysOfWeek.isEmpty()) {
            return after.plusWeeks(interval.toLong())
        }
        val sortedDays = rule.daysOfWeek.filter { it in 1..7 }.sorted()
        if (sortedDays.isEmpty()) return after.plusWeeks(interval.toLong())

        var candidate = after.plusDays(1)
        // Bounded scan: at most interval weeks + 7 days out, so this always terminates.
        val limit = after.plusWeeks(interval.toLong() + 1L)
        while (candidate.isBefore(limit)) {
            if (candidate.dayOfWeek.value in sortedDays) return candidate
            candidate = candidate.plusDays(1)
        }
        // Fallback (should not normally be reached): first configured weekday, interval weeks out.
        return after.plusWeeks(interval.toLong()).with(DayOfWeek.of(sortedDays.first()))
    }

    /**
     * Monthly, clamped: day-of-month is clamped to the target month's length (Jan 31 "monthly"
     * lands on Feb 28/29, Apr 30, ...) rather than skipping the month or overflowing into the
     * next one - matches how Google Calendar's monthly-by-date recurrence behaves.
     */
    private fun nextMonthlyClamped(after: ZonedDateTime, rule: RecurrenceRule, interval: Int): ZonedDateTime {
        val targetDay = rule.dayOfMonth ?: after.dayOfMonth
        val advanced = after.plusMonths(interval.toLong())
        val clampedDay = targetDay.coerceIn(1, advanced.toLocalDate().lengthOfMonth())
        return advanced.withDayOfMonth(clampedDay)
    }

    /** Yearly, clamped: Feb 29 on a non-leap target year clamps to Feb 28 (same policy as monthly). */
    private fun nextYearlyClamped(after: ZonedDateTime, interval: Int): ZonedDateTime {
        val advanced = after.plusYears(interval.toLong())
        return if (after.monthValue == 2 && after.dayOfMonth == 29 && !advanced.toLocalDate().isLeapYear) {
            advanced.withDayOfMonth(28)
        } else {
            advanced
        }
    }
}
