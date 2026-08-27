package com.sotext.data.context

import com.sotext.data.sms.OtpHelper
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.Month
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/**
 * On-device, regex-based extraction of "actionable context" from an SMS/MMS body: dates &
 * times, street addresses, phone numbers, links, shipment tracking numbers, and verification
 * codes. Everything here runs locally against the message text - no network calls, nothing
 * leaves the device.
 *
 * Each detector is deliberately conservative, requiring either distinctive structure (a "1Z"
 * UPS prefix, a full street-suffix address) or a supporting keyword - mirroring the
 * keyword-plus-pattern heuristic [OtpHelper] already uses for OTP codes - to keep false
 * positives low. Earlier, more specific detectors "claim" their matched range so a later,
 * broader detector (e.g. the date parser) can't also fire on the same text.
 */
object MessageContextParser {

    private const val MAX_CARDS_PER_MESSAGE = 3

    private data class Span(val range: IntRange, val value: String)

    fun extract(
        messageId: Long,
        body: String,
        timestampMillis: Long,
        senderAddress: String? = null
    ): List<ContextCard> {
        if (body.isBlank()) return emptyList()

        val claimed = mutableListOf<IntRange>()
        val cards = mutableListOf<ContextCard>()

        findLink(body)?.let { span ->
            claimed += span.range
            cards += ContextCard.Link(
                id = cardId(messageId, "link", span.value),
                matchedText = span.value,
                url = normalizeUrl(span.value)
            )
        }

        findPhone(body, claimed, senderAddress)?.let { span ->
            claimed += span.range
            cards += ContextCard.Phone(
                id = cardId(messageId, "phone", span.value),
                matchedText = span.value,
                number = span.value.trim()
            )
        }

        findTracking(body, claimed)?.let { (span, carrier, url) ->
            claimed += span.range
            cards += ContextCard.Tracking(
                id = cardId(messageId, "track", span.value),
                matchedText = span.value,
                number = span.value.trim(),
                carrier = carrier,
                trackingUrl = url
            )
        }

        findOtp(body, senderAddress, claimed)?.let { span ->
            claimed += span.range
            cards += ContextCard.VerificationCode(
                id = cardId(messageId, "otp", span.value),
                matchedText = span.value,
                code = span.value
            )
        }

        findAddress(body, claimed)?.let { span ->
            claimed += span.range
            cards += ContextCard.Place(
                id = cardId(messageId, "place", span.value),
                matchedText = span.value.trim(),
                query = span.value.trim()
            )
        }

        findEvent(messageId, body, timestampMillis, claimed)?.let { cards += it }

        return cards.take(MAX_CARDS_PER_MESSAGE)
    }

    private fun cardId(messageId: Long, kind: String, value: String) =
        "$messageId:$kind:${value.hashCode()}"

    private fun overlaps(range: IntRange, claimed: List<IntRange>) =
        claimed.any { it.first <= range.last && range.first <= it.last }

    private fun trimTrailingPunctuation(value: String): String =
        value.trimEnd('.', ',', '!', '?', ')', ']', '"', '\'')

    private fun digitsOnly(value: String) = value.filter { it.isDigit() }

    // ---------------------------------------------------------------------
    // Links
    // ---------------------------------------------------------------------

    private val urlRegex = Regex(
        """(?i)\b(?:(?:https?://|www\.)\S+|[a-z0-9][a-z0-9-]*\.(?:com|net|org|io|co|gov|edu|info|biz|app|link|shop|store)(?:/\S*)?)"""
    )

    private fun findLink(body: String): Span? {
        val match = urlRegex.find(body) ?: return null
        val trimmed = trimTrailingPunctuation(match.value)
        if (trimmed.isBlank()) return null
        val start = match.range.first
        return Span(start..(start + trimmed.length - 1), trimmed)
    }

    private fun normalizeUrl(raw: String): String =
        if (raw.startsWith("http://", true) || raw.startsWith("https://", true)) raw else "https://$raw"

    // ---------------------------------------------------------------------
    // Phone numbers (NANP-style: optional +1, area code, 3, 4)
    // ---------------------------------------------------------------------

    private val phoneRegex = Regex(
        """(?<!\d)(?:\+?1[-.\s]?)?\(?([2-9]\d{2})\)?[-.\s]?(\d{3})[-.\s]?(\d{4})(?!\d)"""
    )

    private fun findPhone(body: String, claimed: List<IntRange>, senderAddress: String?): Span? {
        val selfDigits = senderAddress?.let { digitsOnly(it) }?.takeLast(10)
        for (match in phoneRegex.findAll(body)) {
            if (overlaps(match.range, claimed)) continue
            val digits = digitsOnly(match.value)
            if (!selfDigits.isNullOrBlank() && digits.takeLast(10) == selfDigits) continue
            return Span(match.range, match.value)
        }
        return null
    }

    // ---------------------------------------------------------------------
    // Shipment tracking numbers - only the carriers with distinctive-enough
    // formats to avoid colliding with phone numbers/OTP codes.
    // ---------------------------------------------------------------------

    private val upsRegex = Regex("""(?i)\b1Z[0-9A-Z]{16}\b""")
    private val uspsRegex = Regex("""\b(?:94|93|92|82)\d{18,20}\b""")
    private val shippingKeywords = listOf(
        "track", "tracking", "shipped", "shipment", "package", "delivered", "delivery", "usps", "courier"
    )

    private fun findTracking(body: String, claimed: List<IntRange>): Triple<Span, String, String>? {
        upsRegex.find(body)?.let { match ->
            if (!overlaps(match.range, claimed)) {
                return Triple(
                    Span(match.range, match.value),
                    "UPS",
                    "https://www.ups.com/track?tracknum=${match.value}"
                )
            }
        }
        val hasShippingKeyword = shippingKeywords.any { body.contains(it, ignoreCase = true) }
        if (hasShippingKeyword) {
            uspsRegex.find(body)?.let { match ->
                if (!overlaps(match.range, claimed)) {
                    return Triple(
                        Span(match.range, match.value),
                        "USPS",
                        "https://tools.usps.com/go/TrackConfirmAction?tLabels=${match.value}"
                    )
                }
            }
        }
        return null
    }

    // ---------------------------------------------------------------------
    // Verification / one-time codes - defers to OtpHelper's existing
    // keyword-plus-digits heuristic rather than re-implementing it.
    // ---------------------------------------------------------------------

    private fun findOtp(body: String, senderAddress: String?, claimed: List<IntRange>): Span? {
        if (!OtpHelper.isOtpMessage(senderAddress, body)) return null
        val code = OtpHelper.extractCode(body) ?: return null
        val index = body.indexOf(code)
        if (index < 0) return null
        val range = index..(index + code.length - 1)
        if (overlaps(range, claimed)) return null
        return Span(range, code)
    }

    // ---------------------------------------------------------------------
    // Street addresses (US-style: house number + street name + suffix)
    // ---------------------------------------------------------------------

    private val addressRegex = Regex(
        """(?i)\b\d{1,6}\s+[A-Za-z0-9.'-]+(?:\s+[A-Za-z0-9.'-]+){0,3}\s+""" +
            """(?:street|st|avenue|ave|boulevard|blvd|road|rd|lane|ln|drive|dr|court|ct|way|""" +
            """place|pl|circle|cir|highway|hwy|parkway|pkwy|terrace|ter|square|sq)\.?""" +
            """(?:\s*,?\s*[A-Za-z\s]{2,30},?\s*[A-Z]{2}\s*\d{5}(?:-\d{4})?)?"""
    )

    private fun findAddress(body: String, claimed: List<IntRange>): Span? {
        val match = addressRegex.find(body) ?: return null
        if (overlaps(match.range, claimed)) return null
        return Span(match.range, match.value)
    }

    // ---------------------------------------------------------------------
    // Dates & times -> calendar event
    // ---------------------------------------------------------------------

    private val timeRegex = Regex(
        """(?i)\b(?:noon|midnight|([01]?\d)(:([0-5]\d))?\s?(am|pm))\b"""
    )

    private val numericDateRegex = Regex(
        """\b(0?[1-9]|1[0-2])/(0?[1-9]|[12]\d|3[01])(?:/(\d{4}|\d{2}))?\b"""
    )

    private val monthNames = mapOf(
        "january" to Month.JANUARY, "jan" to Month.JANUARY,
        "february" to Month.FEBRUARY, "feb" to Month.FEBRUARY,
        "march" to Month.MARCH, "mar" to Month.MARCH,
        "april" to Month.APRIL, "apr" to Month.APRIL,
        "may" to Month.MAY,
        "june" to Month.JUNE, "jun" to Month.JUNE,
        "july" to Month.JULY, "jul" to Month.JULY,
        "august" to Month.AUGUST, "aug" to Month.AUGUST,
        "september" to Month.SEPTEMBER, "sept" to Month.SEPTEMBER, "sep" to Month.SEPTEMBER,
        "october" to Month.OCTOBER, "oct" to Month.OCTOBER,
        "november" to Month.NOVEMBER, "nov" to Month.NOVEMBER,
        "december" to Month.DECEMBER, "dec" to Month.DECEMBER
    )
    private val monthDateRegex = Regex(
        "(?i)\\b(" + monthNames.keys.joinToString("|") + ")\\.?\\s+([0-3]?\\d)(?:st|nd|rd|th)?(?:,?\\s+(\\d{4}))?\\b"
    )

    private val weekdayNames = mapOf(
        "monday" to DayOfWeek.MONDAY, "mon" to DayOfWeek.MONDAY,
        "tuesday" to DayOfWeek.TUESDAY, "tues" to DayOfWeek.TUESDAY, "tue" to DayOfWeek.TUESDAY,
        "wednesday" to DayOfWeek.WEDNESDAY, "wed" to DayOfWeek.WEDNESDAY,
        "thursday" to DayOfWeek.THURSDAY, "thurs" to DayOfWeek.THURSDAY, "thu" to DayOfWeek.THURSDAY,
        "friday" to DayOfWeek.FRIDAY, "fri" to DayOfWeek.FRIDAY,
        "saturday" to DayOfWeek.SATURDAY, "sat" to DayOfWeek.SATURDAY,
        "sunday" to DayOfWeek.SUNDAY, "sun" to DayOfWeek.SUNDAY
    )
    private val weekdayRegex = Regex(
        "(?i)\\b(next\\s+)?(" + weekdayNames.keys.joinToString("|") + ")\\b"
    )

    private val relativeDayRegex = Regex("""(?i)\b(today|tomorrow|tonight)\b""")

    private fun findEvent(
        messageId: Long,
        body: String,
        timestampMillis: Long,
        claimed: List<IntRange>
    ): ContextCard.Event? {
        val zone = ZoneId.systemDefault()
        val anchor = Instant.ofEpochMilli(timestampMillis).atZone(zone).toLocalDate()

        val timeMatch = timeRegex.find(body)?.takeIf { !overlaps(it.range, claimed) }
        val time = timeMatch?.let { parseTime(it) }

        val dateMatches = mutableListOf<Pair<IntRange, LocalDate>>()
        numericDateRegex.find(body)?.takeIf { !overlaps(it.range, claimed) }?.let { m ->
            parseNumericDate(m, anchor)?.let { dateMatches += m.range to it }
        }
        monthDateRegex.find(body)?.takeIf { !overlaps(it.range, claimed) }?.let { m ->
            parseMonthDate(m, anchor)?.let { dateMatches += m.range to it }
        }
        weekdayRegex.find(body)?.takeIf { !overlaps(it.range, claimed) }?.let { m ->
            dateMatches += m.range to parseWeekday(m, anchor)
        }
        // "today"/"tomorrow"/"tonight" are too common as generic filler ("thanks for today") to
        // justify a calendar card on their own - only count them when paired with an explicit time.
        if (time != null) {
            relativeDayRegex.find(body)?.takeIf { !overlaps(it.range, claimed) }?.let { m ->
                val date = if (m.value.equals("tomorrow", ignoreCase = true)) anchor.plusDays(1) else anchor
                dateMatches += m.range to date
            }
        }

        val bestDate = dateMatches.minByOrNull { it.first.first }
        if (bestDate == null && time == null) return null

        val date = bestDate?.second ?: anchor
        val matchedRanges = listOfNotNull(bestDate?.first, timeMatch?.range).sortedBy { it.first }
        val matchedText = matchedRanges.joinToString(" ") { body.substring(it.first, it.last + 1) }
            .ifBlank { date.toString() }

        val allDay = time == null
        // All-day events are conventionally anchored to local midnight; a specific time always wins.
        val startTime = time ?: LocalTime.MIDNIGHT
        val startMillis = date.atTime(startTime).atZone(zone).toInstant().toEpochMilli()
        val endMillis = if (allDay) {
            date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        } else {
            date.atTime(startTime).plusHours(1).atZone(zone).toInstant().toEpochMilli()
        }

        return ContextCard.Event(
            id = cardId(messageId, "event", "$startMillis"),
            matchedText = matchedText,
            title = matchedText.replaceFirstChar { it.uppercase() },
            startMillis = startMillis,
            endMillis = endMillis,
            allDay = allDay
        )
    }

    private fun parseTime(match: MatchResult): LocalTime {
        val value = match.value.lowercase()
        if (value.contains("noon")) return LocalTime.NOON
        if (value.contains("midnight")) return LocalTime.MIDNIGHT
        val hourGroup = match.groupValues.getOrNull(1)?.toIntOrNull() ?: 12
        val minuteGroup = match.groupValues.getOrNull(3)?.toIntOrNull() ?: 0
        val isPm = match.groupValues.getOrNull(4)?.equals("pm", ignoreCase = true) == true
        var hour = hourGroup % 12
        if (isPm) hour += 12
        return runCatching { LocalTime.of(hour, minuteGroup) }.getOrDefault(LocalTime.NOON)
    }

    private fun parseNumericDate(match: MatchResult, anchor: LocalDate): LocalDate? {
        val month = match.groupValues[1].toIntOrNull() ?: return null
        val day = match.groupValues[2].toIntOrNull() ?: return null
        val yearRaw = match.groupValues.getOrNull(3)
        val year = when {
            yearRaw.isNullOrBlank() -> anchor.year
            yearRaw.length == 2 -> 2000 + yearRaw.toInt()
            else -> yearRaw.toInt()
        }
        return runCatching { LocalDate.of(year, month, day) }.getOrNull()
    }

    private fun parseMonthDate(match: MatchResult, anchor: LocalDate): LocalDate? {
        val month = monthNames[match.groupValues[1].lowercase()] ?: return null
        val day = match.groupValues[2].toIntOrNull() ?: return null
        val yearRaw = match.groupValues.getOrNull(3)
        val year = if (yearRaw.isNullOrBlank()) anchor.year else yearRaw.toIntOrNull() ?: anchor.year
        return runCatching { LocalDate.of(year, month, day) }.getOrNull()
    }

    private fun parseWeekday(match: MatchResult, anchor: LocalDate): LocalDate {
        val hasNext = match.groupValues.getOrNull(1)?.isNotBlank() == true
        val target = weekdayNames[match.groupValues[2].lowercase()] ?: anchor.dayOfWeek
        var date = anchor.with(TemporalAdjusters.nextOrSame(target))
        if (hasNext) date = date.plusWeeks(1)
        return date
    }
}
