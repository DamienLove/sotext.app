package com.sotext.data.context

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class MessageContextParserTest {

    private val zone = ZoneId.systemDefault()
    private val anchorDate: LocalDate = LocalDate.of(2026, 8, 27)
    private val anchorMillis = anchorDate.atStartOfDay(zone).toInstant().toEpochMilli()

    @Test
    fun `plain conversational message yields no cards`() {
        val cards = MessageContextParser.extract(1, "Hey, how are you doing today?", anchorMillis)
        assertTrue(cards.isEmpty())
    }

    @Test
    fun `detects a phone number and formats a call card`() {
        val cards = MessageContextParser.extract(
            messageId = 1,
            body = "Call the office at 555-123-4567 when you get a chance.",
            timestampMillis = anchorMillis
        )
        val phone = cards.filterIsInstance<ContextCard.Phone>().singleOrNull()
        assertNotNull("expected a phone card", phone)
        assertEquals("555-123-4567", phone!!.number)
    }

    @Test
    fun `does not surface the thread's own number as a phone card`() {
        val cards = MessageContextParser.extract(
            messageId = 1,
            body = "It's me, 5551234567",
            timestampMillis = anchorMillis,
            senderAddress = "+15551234567"
        )
        assertTrue(cards.filterIsInstance<ContextCard.Phone>().isEmpty())
    }

    @Test
    fun `detects a link`() {
        val cards = MessageContextParser.extract(
            messageId = 1,
            body = "Check this out: https://example.com/page?x=1 thanks!",
            timestampMillis = anchorMillis
        )
        val link = cards.filterIsInstance<ContextCard.Link>().singleOrNull()
        assertNotNull("expected a link card", link)
        assertEquals("https://example.com/page?x=1", link!!.url)
    }

    @Test
    fun `detects a verification code via the OTP heuristic`() {
        val cards = MessageContextParser.extract(
            messageId = 1,
            body = "Your verification code is 482913. Do not share it.",
            timestampMillis = anchorMillis,
            senderAddress = "28107"
        )
        val otp = cards.filterIsInstance<ContextCard.VerificationCode>().singleOrNull()
        assertNotNull("expected a verification code card", otp)
        assertEquals("482913", otp!!.code)
    }

    @Test
    fun `detects a UPS tracking number without needing a keyword`() {
        val cards = MessageContextParser.extract(
            messageId = 1,
            body = "Your order is on its way: 1Z999AA10123456784",
            timestampMillis = anchorMillis
        )
        val tracking = cards.filterIsInstance<ContextCard.Tracking>().singleOrNull()
        assertNotNull("expected a tracking card", tracking)
        assertEquals("UPS", tracking!!.carrier)
    }

    @Test
    fun `detects a street address without a bare 'tomorrow' spawning a noisy event card`() {
        val cards = MessageContextParser.extract(
            messageId = 1,
            body = "Meet me at 123 Main Street tomorrow",
            timestampMillis = anchorMillis
        )
        val place = cards.filterIsInstance<ContextCard.Place>().singleOrNull()
        assertNotNull("expected a place card", place)
        assertEquals("123 Main Street", place!!.query)

        // "tomorrow" with no explicit time is too weak a signal on its own (too common as filler,
        // e.g. "thanks for today") to justify a calendar card.
        assertTrue(cards.filterIsInstance<ContextCard.Event>().isEmpty())
    }

    @Test
    fun `an explicit month date with no time produces an all-day event anchored at local midnight`() {
        val cards = MessageContextParser.extract(
            messageId = 1,
            body = "The invoice is due September 3rd",
            timestampMillis = anchorMillis
        )
        val event = cards.filterIsInstance<ContextCard.Event>().singleOrNull()
        assertNotNull("expected an event card", event)
        assertTrue(event!!.allDay)
        val expectedStart = LocalDate.of(anchorDate.year, 9, 3).atStartOfDay(zone).toInstant().toEpochMilli()
        assertEquals(expectedStart, event.startMillis)
    }

    @Test
    fun `combines a weekday and a time into a single dated event`() {
        val weekdayWord = anchorDate.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
        val cards = MessageContextParser.extract(
            messageId = 1,
            body = "Let's grab dinner $weekdayWord at 5:30pm",
            timestampMillis = anchorMillis
        )
        val event = cards.filterIsInstance<ContextCard.Event>().singleOrNull()
        assertNotNull("expected an event card", event)
        assertFalse(event!!.allDay)
        val expectedStart = anchorDate.atTime(LocalTime.of(17, 30)).atZone(zone).toInstant().toEpochMilli()
        assertEquals(expectedStart, event.startMillis)
    }

    @Test
    fun `resolves a numeric date with a noon time`() {
        val cards = MessageContextParser.extract(
            messageId = 1,
            body = "See you on 12/25 at noon",
            timestampMillis = anchorMillis
        )
        val event = cards.filterIsInstance<ContextCard.Event>().singleOrNull()
        assertNotNull("expected an event card", event)
        val expectedStart = LocalDate.of(anchorDate.year, 12, 25)
            .atTime(LocalTime.NOON)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
        assertEquals(expectedStart, event!!.startMillis)
    }

    @Test
    fun `caps the number of cards per message`() {
        val cards = MessageContextParser.extract(
            messageId = 1,
            body = "Call 555-123-4567, visit https://example.com, ship to 500 Oak Avenue, " +
                "tracking 1Z999AA10123456784, and let's meet tomorrow at 3pm.",
            timestampMillis = anchorMillis
        )
        assertTrue(cards.size <= 3)
    }
}
