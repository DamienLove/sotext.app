package com.sotext.data.intelligence

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class MessageIntentDetectorTest {

    private val zone = ZoneId.systemDefault()
    private val anchorDate: LocalDate = LocalDate.of(2026, 8, 27)
    private val anchorMillis = anchorDate.atStartOfDay(zone).toInstant().toEpochMilli()

    private fun analyze(body: String) = MessageIntentDetector.analyze(1, body, anchorMillis)

    @Test
    fun `explicit reminder is detected with high confidence and its entities`() {
        val matches = analyze("Remind me to call the dentist tomorrow at 10am.")
        val reminder = matches.firstOrNull()
        assertNotNull("expected a reminder match", reminder)
        assertEquals(MessageIntent.REMINDER, reminder!!.intent)
        assertEquals(Actionability.EXPLICIT, reminder.actionability)
        assertTrue("explicit reminder should be high confidence", reminder.confidence >= MessageIntelligenceThresholds.DEFAULT.highConfidence)
        assertEquals("call the dentist", reminder.entities.task)
        assertTrue("expected a resolved date/time", reminder.entities.hasDateTime())
    }

    @Test
    fun `implied reminder without a date does not reach card-worthy confidence`() {
        // Spec's own example of task-like language that should NOT auto-create a reminder.
        val matches = analyze("I need to remember to buy milk.")
        val reminder = matches.firstOrNull { it.intent == MessageIntent.REMINDER }
        assertNotNull("expected an implied reminder match", reminder)
        assertEquals(Actionability.IMPLIED, reminder!!.actionability)
        assertTrue(
            "implied reminder without a date/time should be below the medium threshold",
            reminder.confidence < MessageIntelligenceThresholds.DEFAULT.mediumConfidence
        )
    }

    @Test
    fun `an ordinary statement with no task language yields no matches`() {
        val matches = analyze("I bought milk today.")
        assertTrue(matches.isEmpty())
    }

    @Test
    fun `explicit scheduling request is detected`() {
        // Spec's own example.
        val matches = analyze("Are you free Saturday at 6?")
        val scheduling = matches.firstOrNull()
        assertNotNull(scheduling)
        assertEquals(MessageIntent.SCHEDULING, scheduling!!.intent)
        assertEquals(Actionability.EXPLICIT, scheduling.actionability)
        assertTrue(scheduling.confidence >= MessageIntelligenceThresholds.DEFAULT.highConfidence)
    }

    @Test
    fun `scheduling language without a question mark is ambiguous, not explicit`() {
        val matches = analyze("Can we meet Saturday")
        val scheduling = matches.firstOrNull { it.intent == MessageIntent.SCHEDULING }
        assertNotNull(scheduling)
        assertEquals(Actionability.AMBIGUOUS, scheduling!!.actionability)
        val thresholds = MessageIntelligenceThresholds.DEFAULT
        assertTrue(scheduling.confidence >= thresholds.mediumConfidence && scheduling.confidence < thresholds.highConfidence)
    }

    @Test
    fun `location request is detected`() {
        // Spec's own example.
        val matches = analyze("Where are we meeting tonight?")
        val location = matches.firstOrNull()
        assertNotNull(location)
        assertEquals(MessageIntent.LOCATION_REQUEST, location!!.intent)
        assertEquals(Actionability.EXPLICIT, location.actionability)
    }

    @Test
    fun `contact request extracts the person entity`() {
        // Spec's own example.
        val matches = analyze("Can you send me Sarah's number?")
        val contact = matches.firstOrNull()
        assertNotNull(contact)
        assertEquals(MessageIntent.CONTACT_REQUEST, contact!!.intent)
        assertEquals(Actionability.EXPLICIT, contact.actionability)
        assertEquals("Sarah", contact.entities.person)
    }

    @Test
    fun `payment request extracts the amount`() {
        // Spec's own example.
        val matches = analyze("Can you send me \$25 for dinner?")
        val payment = matches.firstOrNull()
        assertNotNull(payment)
        assertEquals(MessageIntent.PAYMENT_REQUEST, payment!!.intent)
        assertEquals(Actionability.EXPLICIT, payment.actionability)
        assertEquals(25.0, payment.entities.amount!!, 0.001)
    }

    @Test
    fun `payment request without an amount is only medium confidence`() {
        val matches = analyze("Can you send me the money?")
        val payment = matches.firstOrNull { it.intent == MessageIntent.PAYMENT_REQUEST }
        assertNotNull(payment)
        assertNull(payment!!.entities.amount)
        val thresholds = MessageIntelligenceThresholds.DEFAULT
        assertTrue(payment.confidence >= thresholds.mediumConfidence && payment.confidence < thresholds.highConfidence)
    }

    @Test
    fun `a message can carry a primary reminder and a secondary information request`() {
        // Spec section 6's own example.
        val matches = analyze("Remind me tomorrow to pick up Sarah at the airport, and what time does her flight land?")
        assertEquals(MessageIntent.REMINDER, matches.first().intent)
        assertEquals(
            "a time phrase between \"remind me\" and \"to\" should not swallow the task",
            "pick up Sarah",
            matches.first().entities.task
        )
        val secondary = matches.drop(1).firstOrNull { it.intent != MessageIntent.REMINDER }
        assertNotNull("expected a secondary, non-reminder match", secondary)
        assertEquals(MessageIntent.INFORMATION_REQUEST, secondary!!.intent)
    }

    @Test
    fun `an explicit reminder with no date or time leaves those entities null rather than guessing`() {
        val matches = analyze("Remind me to call the dentist.")
        val reminder = matches.first()
        assertEquals(MessageIntent.REMINDER, reminder.intent)
        assertEquals(Actionability.EXPLICIT, reminder.actionability)
        assertEquals("call the dentist", reminder.entities.task)
        assertFalse("no date/time text was present, so none should be invented", reminder.entities.hasDateTime())
    }

    @Test
    fun `a plain question with no other signal is a low-priority information request`() {
        val matches = analyze("What time does her flight land?")
        val info = matches.firstOrNull()
        assertNotNull(info)
        assertEquals(MessageIntent.INFORMATION_REQUEST, info!!.intent)
        val thresholds = MessageIntelligenceThresholds.DEFAULT
        assertTrue(
            "a generic question should sit in the suggestion tier, not auto-card",
            info.confidence >= thresholds.mediumConfidence && info.confidence < thresholds.highConfidence
        )
    }

    // -----------------------------------------------------------------------
    // Scheduled Messages: analyzeDraft (draft-text-only, never runs on inbound messages)
    // -----------------------------------------------------------------------

    private fun analyzeDraft(draft: String) = MessageIntentDetector.analyzeDraft(draft, anchorMillis)

    @Test
    fun `draft with a trigger phrase and a resolvable time suggests SCHEDULE`() {
        // MessageContextParser only resolves "tomorrow" into a date when it's paired with an
        // explicit clock time (a bare "tomorrow morning" is too common as filler on its own) -
        // matches its existing behavior for inbound messages, reused as-is here.
        val match = analyzeDraft("I'll send this tomorrow at 9am once I have the address.")
        assertNotNull("expected a schedule suggestion", match)
        assertEquals(MessageIntent.SCHEDULE, match!!.intent)
        assertEquals(Actionability.EXPLICIT, match.actionability)
        assertTrue("expected a resolved date/time", match.entities.hasDateTime())
    }

    @Test
    fun `draft with a date but no trigger phrase does not suggest scheduling`() {
        // Plenty of ordinary drafts mention tomorrow without meaning "send this later".
        val match = analyzeDraft("See you tomorrow at the usual spot!")
        assertNull("a bare date/time mention alone must not suggest scheduling", match)
    }

    @Test
    fun `draft with a trigger phrase but no resolvable time suggests nothing`() {
        // "schedule this" alone, with nothing MessageContextParser can resolve to a date/time,
        // has nothing concrete to prefill - never guess a time, just stay silent.
        val match = analyzeDraft("Schedule this for later, not sure when yet.")
        assertNull(match)
    }

    @Test
    fun `blank draft suggests nothing`() {
        assertNull(analyzeDraft(""))
        assertNull(analyzeDraft("   "))
    }

    @Test
    fun `analyzeDraft never mutates any state - it only returns data`() {
        // Structural guarantee behind "never automatically schedule an ambiguous message without
        // user confirmation": calling analyzeDraft repeatedly must be a pure, side-effect-free
        // read, so the only path from a suggestion to an actual ScheduledMessage row is the UI
        // layer's explicit "Schedule" tap opening ScheduleMessageSheet.
        val draft = "I'll send this tomorrow at 9am."
        val first = analyzeDraft(draft)
        val second = analyzeDraft(draft)
        assertEquals(first, second)
    }
}
